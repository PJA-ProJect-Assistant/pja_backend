package com.project.PJA.workspace.controller;

import com.project.PJA.common.dto.SuccessResponse;
import com.project.PJA.invitation.service.InvitationService;
import com.project.PJA.user.entity.Users;
import com.project.PJA.workspace.dto.*;
import com.project.PJA.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {
    private final WorkspaceService workspaceService;
    private final InvitationService invitationService;

    // 사용자의 워크스페이스 전체 조회
    @GetMapping({ "", "/" })
    public ResponseEntity<SuccessResponse<List<WorkspaceResponse>>> getMyWorkspaces(@AuthenticationPrincipal Users user) {
        log.info("=== workspace 생성 api 진입 == username: {}", user.getUsername());
        Long userId = user.getUserId();
        List<WorkspaceResponse> userWorkspaceList = workspaceService.getMyWorkspaces(userId);

        SuccessResponse<List<WorkspaceResponse>> response = new SuccessResponse<>(
                "success","워크스페이스 정보를 성공적으로 조회했습니다.", userWorkspaceList
        );

        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    // 워크스페이스 생성
    @PostMapping({ "", "/" })
    public ResponseEntity<SuccessResponse<WorkspaceResponse>> createWorkspace(@AuthenticationPrincipal Users user,
                                                                              @RequestBody WorkspaceCreateRequest workspaceCreateRequest) {
        Long userId = user.getUserId();
        WorkspaceResponse savedWorkspace = workspaceService.createWorkspace(userId, workspaceCreateRequest);

        SuccessResponse<WorkspaceResponse> response = new SuccessResponse<>(
                "success", "워크스페이스가 생성되었습니다.", savedWorkspace
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    // 워크스페이스 수정
    @PutMapping("/{workspaceId}")
    public ResponseEntity<SuccessResponse<WorkspaceResponse>> updateWorkspace(@AuthenticationPrincipal Users user,
                                @PathVariable Long workspaceId,
                                @RequestBody WorkspaceUpdateRequest workspaceUpdateRequest) {
        Long userId = user.getUserId();
        WorkspaceResponse updatedWorkspace = workspaceService.updateWorkspace(userId, workspaceId, workspaceUpdateRequest);

        SuccessResponse<WorkspaceResponse> response = new SuccessResponse<>(
                "success", "워크스페이스가 성공적으로 수정되었습니다.", updatedWorkspace
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 워크스페이스 진행도 완료 수정
    @PatchMapping("/{workspaceId}/complete")
    public ResponseEntity<SuccessResponse<WorkspaceResponse>> updateCompletionStatus(@AuthenticationPrincipal Users user,
                                                                                     @PathVariable Long workspaceId,
                                                                                     @RequestBody WorkspaceProgressStep workspaceProgressStep) {
        Long userId = user.getUserId();
        WorkspaceResponse updatedWorkspace = workspaceService.updateCompletionStatus(userId, workspaceId, workspaceProgressStep);

        SuccessResponse<WorkspaceResponse> response = new SuccessResponse<>(
                "success", "프로젝트가 성공적으로 수정되었습니다.", updatedWorkspace
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 워크스페이스 삭제
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<SuccessResponse<WorkspaceResponse>> deleteWorkspace(@AuthenticationPrincipal Users user,
                                                                              @PathVariable Long workspaceId) {
        Long userId = user.getUserId();
        WorkspaceResponse deletedWorkspace = workspaceService.deleteWorkspace(userId, workspaceId);

        SuccessResponse<WorkspaceResponse> response = new SuccessResponse<>(
                "success", "워크스페이스가 성공적으로 삭제되었습니다.", deletedWorkspace
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 워크스페이스 팀원 초대 메일 전송
    @PostMapping("/{workspaceId}/invite")
    public ResponseEntity<SuccessResponse<WorkspaceInviteResponse>> inviteUserToWorkspace(@AuthenticationPrincipal Users user,
                                                                       @PathVariable Long workspaceId,
                                                                       @RequestBody WorkspaceInviteRequest workspaceInviteRequest) {
        Long userId = user.getUserId();
        WorkspaceInviteResponse workspaceInviteResponse = invitationService.sendInvitation(userId, workspaceId, workspaceInviteRequest);

        SuccessResponse<WorkspaceInviteResponse> response = new SuccessResponse<>(
                "success", "팀원 초대 이메일이 성공적으로 전송됐습니다.", workspaceInviteResponse
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
