package com.project.PJA.notification.service;

import com.project.PJA.exception.NotFoundException;
import com.project.PJA.notification.dto.NotiReadResponseDto;
import com.project.PJA.notification.entity.Notification;
import com.project.PJA.notification.entity.UserNotification;
import com.project.PJA.notification.repository.NotificationRepository;
import com.project.PJA.notification.repository.UserNotificationRepository;
import com.project.PJA.project_progress.entity.Action;
import com.project.PJA.project_progress.entity.ActionParticipant;
import com.project.PJA.project_progress.entity.ActionPost;
import com.project.PJA.sse.repository.SseEmitterRepository;
import com.project.PJA.user.entity.Users;
import com.project.PJA.workspace.entity.WorkspaceMember;
import com.project.PJA.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final WorkspaceService workspaceService;
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final SseEmitterRepository sseEmitterRepository;

    // 알림 전체 가져오기
    @Transactional(readOnly = true)
    public List<NotiReadResponseDto> getNotiList(Users user, Long workspaceId) {
        workspaceService.authorizeOwnerOrMemberOrThrow(user.getUserId(), workspaceId, "이 워크스페이스의 알림을 조회할 권한이 없습니다.");

        // 해당 유저의 알림 리스트 가져오기
        List<UserNotification> userNotiList = userNotificationRepository.findByReceiverAndWorkspaceIdOrderByCreatedAtDesc(user, workspaceId);

        List<NotiReadResponseDto> notiList = new ArrayList<>();
        for (UserNotification userNoti : userNotiList) {
            Notification notification = userNoti.getNotification();
            NotiReadResponseDto notiDto = new NotiReadResponseDto();

            notiDto.setNotificationId(notification.getNotificationId());
            notiDto.setMessage(notification.getMessage());
            notiDto.setRead(userNoti.isRead());
            notiDto.setActionPostId(
                    notification.getActionPost() != null ? notification.getActionPost().getActionPostId() : null
            );
            notiDto.setCreatedAt(notification.getCreatedAt());

            notiList.add(notiDto);
        }

        return notiList;
    }

    // 읽지 않은 알림 존재 여부 반환
    @Transactional(readOnly = true)
    public boolean getNotReadNotification(Users user, Long workspaceId) {
        workspaceService.authorizeOwnerOrMemberOrThrow(user.getUserId(), workspaceId, "이 워크스페이스의 읽지 않은 알림을 조회할 권한이 없습니다.");

        return userNotificationRepository.existsByReceiverAndWorkspaceIdAndIsReadFalse(user, workspaceId);
    }

    // 읽지 않은 알림 개수 반환
    @Transactional(readOnly = true)
    public long countUnreadNotification(Users user, Long workspaceId) {
        workspaceService.authorizeOwnerOrMemberOrThrow(user.getUserId(), workspaceId, "이 워크스페이스의 읽지 않은 알림을 조회할 권한이 없습니다.");

        return userNotificationRepository.countByReceiverAndWorkspaceIdAndIsReadFalse(user, workspaceId);
    }

    // 알림 생성
    @Transactional
    public void createNotification(List<Users> receivers, String message, ActionPost actionPost, Long workspaceId) {
        // 알림 생성
        Notification notification = Notification.builder()
                .message(message)
                .createdAt(LocalDateTime.now())
                .actionPost(actionPost)
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        Action action = actionPost.getAction();
        Set<WorkspaceMember> participants = action.getParticipants().stream()
                .map(ActionParticipant::getWorkspaceMember)
                .collect(Collectors.toSet());

        List<UserNotification> userNotifications = receivers.stream()
                .map(user -> {
                    UserNotification userNotification = UserNotification.builder()
                            .receiver(user)
                            .notification(savedNotification)
                            .isRead(false)
                            .workspaceId(workspaceId)
                            .createdAt(savedNotification.getCreatedAt())
                            .build();

                    savedNotification.addUserNotification(userNotification);
                    return userNotification;
                })
                .collect(Collectors.toList());

        userNotificationRepository.saveAll(userNotifications);

        // SSE 전송
        for(Users receiver : receivers) {
            try {
                sseEmitterRepository.get(workspaceId, receiver.getUserId())
                        .ifPresent(emitter -> {
                            try {
                                NotiReadResponseDto notiDto = NotiReadResponseDto.builder()
                                        .notificationId(savedNotification.getNotificationId())
                                        .message(savedNotification.getMessage())
                                        .isRead(false)
                                        .actionPostId(actionPost.getActionPostId())
                                        .createdAt(savedNotification.getCreatedAt())
                                        .build();

                                emitter.send(SseEmitter.event()
                                        .name("notification")
                                        .data(notiDto));
                            } catch (IOException e) {
                                log.warn("SSE 전송 실패 - emitter 제거: {}", e.getMessage());
                                sseEmitterRepository.delete(workspaceId, receiver.getUserId());
                            }
                        });

            } catch (Exception e) {
                log.error("SSE emitter 조회 중 오류가 발생했습니다: {}", e.getMessage());
            }
        }
    }

    // 알림 개별 읽음 처리
    @Transactional
    public Map<String, Object> readNotification(Users user, Long workspaceId, Long notiId) {
        workspaceService.authorizeOwnerOrMemberOrThrow(user.getUserId(), workspaceId, "이 워크스페이스의 알림을 읽음 처리 할 권한이 없습니다.");

        UserNotification userNotification
                = userNotificationRepository.findByReceiverAndNotification_NotificationId(user, notiId)
                .orElseThrow(()-> new NotFoundException("존재하지 않는 알림입니다."));

        userNotification.setRead(true);

        Notification notification = userNotification.getNotification();

        return Map.of("notiId", notification.getNotificationId(),
                "notiMessage", notification.getMessage(),
                "notiIsRead", userNotification.isRead());
    }

    // 알림 전체 읽음 처리
    @Transactional
    public void readNotificationAll(Users user, Long workspaceId) {
        workspaceService.authorizeOwnerOrMemberOrThrow(user.getUserId(), workspaceId, "이 워크스페이스의 알림을 읽음 처리 할 권한이 없습니다.");

        List<UserNotification> notReadNotiList
                = userNotificationRepository.findByReceiverAndWorkspaceIdAndIsReadFalse(user, workspaceId);

        for(UserNotification userNoti : notReadNotiList) {
            userNoti.setRead(true);
        }
    }

    // 알림 개별 삭제
    @Transactional
    public Map<String, Object> deleteNotification(Users user, Long workspaceId, Long notiId) {
        workspaceService.authorizeOwnerOrMemberOrThrow(user.getUserId(), workspaceId, "이 워크스페이스의 알림을 삭제할 권한이 없습니다.");

        UserNotification userNotification
                = userNotificationRepository.findByReceiverAndNotification_NotificationId(user,notiId)
                .orElseThrow(()->new NotFoundException("존재하지 않는 알림입니다."));


        Notification notification = userNotification.getNotification();

        userNotificationRepository.delete(userNotification);

        if(!userNotificationRepository.existsByNotification(notification)) {
            notificationRepository.delete(notification);
        }

        return Map.of("notiId", notiId);
    }

    // 알림 전체 삭제
    @Transactional
    public void deleteNotificationAll(Users user, Long workspaceId) {
        workspaceService.authorizeOwnerOrMemberOrThrow(user.getUserId(), workspaceId, "이 워크스페이스의 알림을 삭제할 권한이 없습니다.");

       List<UserNotification> userNotificationList
               = userNotificationRepository.findByReceiverAndWorkspaceId(user, workspaceId);

        Set<Notification> notificationSet
                = userNotificationList.stream()
                        .map(UserNotification::getNotification)
                                .collect(Collectors.toSet());

       userNotificationRepository.deleteAll(userNotificationList);

        for(Notification noti : notificationSet) {
            if(!userNotificationRepository.existsByNotification(noti)) {
                notificationRepository.delete(noti);
            }
        }
    }
}
