package com.project.PJA.projectinfo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.PJA.exception.BadRequestException;
import com.project.PJA.exception.NotFoundException;
import com.project.PJA.ideainput.dto.IdeaInputRequest;
import com.project.PJA.ideainput.dto.MainFunctionData;
import com.project.PJA.ideainput.dto.TechStackData;
import com.project.PJA.ideainput.entity.IdeaInput;
import com.project.PJA.ideainput.entity.MainFunction;
import com.project.PJA.ideainput.entity.TechStack;
import com.project.PJA.ideainput.repository.IdeaInputRepository;
import com.project.PJA.ideainput.repository.MainFunctionRepository;
import com.project.PJA.ideainput.repository.TechStackRepository;
import com.project.PJA.projectinfo.dto.*;
import com.project.PJA.projectinfo.entity.ProjectInfo;
import com.project.PJA.projectinfo.repository.ProjectInfoRepository;
import com.project.PJA.requirement.dto.RequirementRequest;
import com.project.PJA.requirement.service.RequirementService;
import com.project.PJA.sse.service.SseService;
import com.project.PJA.user.entity.Users;
import com.project.PJA.workspace.entity.Workspace;
import com.project.PJA.workspace.enumeration.ProgressStep;
import com.project.PJA.workspace.repository.WorkspaceRepository;
import com.project.PJA.workspace.service.WorkspaceService;
import com.project.PJA.workspace_activity.enumeration.ActivityActionType;
import com.project.PJA.workspace_activity.enumeration.ActivityTargetType;
import com.project.PJA.workspace_activity.service.WorkspaceActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectInfoService {
    private final WorkspaceRepository workspaceRepository;
    private final ProjectInfoRepository projectInfoRepository;
    private final IdeaInputRepository ideaInputRepository;
    private final MainFunctionRepository mainFunctionRepository;
    private final TechStackRepository techStackRepository;
    private final RestTemplate restTemplate;
    private final WorkspaceService workspaceService;
    private final RequirementService requirementService;
    private final SseService sseService;
    private final ObjectMapper objectMapper;
    private final WorkspaceActivityService workspaceActivityService;

    // 프로젝트 정보 전체 조회
    @Transactional(readOnly = true)
    public String getAllProjectInfo() {
        List<Workspace> foundWorkspaces = workspaceRepository.findByIsPublicTrue();

        List<Long> foundWorkspaceIds = foundWorkspaces.stream()
                .map(workspace -> workspace.getWorkspaceId())
                .collect(Collectors.toList());
        List<ProjectInfo> projectInfos = projectInfoRepository.findAllByWorkspace_WorkspaceIdIn(foundWorkspaceIds);

        List<PublicProjectInfoResponse> projectInfoResponses = projectInfos.stream()
                .map(projectInfo -> new PublicProjectInfoResponse(
                        projectInfo.getProjectInfoId(),
                        projectInfo.getWorkspace().getWorkspaceId(),
                        projectInfo.getTitle(),
                        projectInfo.getCategory(),
                        projectInfo.getTargetUsers(),
                        projectInfo.getCoreFeatures(),
                        projectInfo.getTechnologyStack(),
                        projectInfo.getProblemSolving()
                ))
                .collect(Collectors.toList());
        String project;

        try {
            project = objectMapper.writeValueAsString(projectInfoResponses);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패: " + e.getMessage(), e);
        }

        return project;
    }

    // 프로젝트 정보 조회
    @Transactional(readOnly = true)
    public ProjectInfoResponse getProjectInfo(Long userId, Long workspaceId) {
        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("요청하신 워크스페이스를 찾을 수 없습니다."));

        workspaceService.validateWorkspaceAccess(userId, foundWorkspace);

        ProjectInfo foundProjectInfo = projectInfoRepository.findByWorkspace_WorkspaceId(workspaceId)
                .orElseThrow(() -> new NotFoundException("요청하신 아이디어를 찾을 수 없습니다."));

        return new ProjectInfoResponse(
                foundProjectInfo.getProjectInfoId(),
                foundProjectInfo.getTitle(),
                foundProjectInfo.getCategory(),
                foundProjectInfo.getTargetUsers(),
                foundProjectInfo.getCoreFeatures(),
                foundProjectInfo.getTechnologyStack(),
                foundProjectInfo.getProblemSolving()
        );
    }

    // 프로젝트 정보 AI 생성
    @Transactional
    public ProjectInfoResponse createProjectInfo(Long userId, Long workspaceId, List<RequirementRequest> requests) {
        requirementService.validateRequirements(requests);

        Workspace foundWorkspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("요청하신 워크스페이스를 찾을 수 없습니다."));

        if (foundWorkspace.getProgressStep() == ProgressStep.ZERO) {
            throw new BadRequestException("AI 생성을 진행하려면 먼저 아이디어를 입력해 주세요.");
        } else if (foundWorkspace.getProgressStep() != ProgressStep.ONE) {
            throw new BadRequestException("이미 프로젝트 정보가 생성되어 AI 생성을 다시 진행할 수 없습니다.");
        }

        workspaceService.authorizeOwnerOrMemberOrThrow(userId, workspaceId, "이 워크스페이스에 생성할 권한이 없습니다.");

        // 아이디어 입력 정보 찾기
        IdeaInput foundIdeaInput = ideaInputRepository.findByWorkspace_WorkspaceId(workspaceId)
                .orElseThrow(() -> new NotFoundException("요청하신 아이디어 입력을 찾을 수 없습니다."));
        List<MainFunction> foundMainFunctions = mainFunctionRepository.findAllByIdeaInput_IdeaInputId(foundIdeaInput.getIdeaInputId());
        List<TechStack> foundTechStacks = techStackRepository.findAllByIdeaInput_IdeaInputId(foundIdeaInput.getIdeaInputId());

        List<MainFunctionData> mainFunctionDataList = foundMainFunctions.stream()
                .map(mainFunction -> new MainFunctionData(
                        mainFunction.getMainFunctionId(),
                        mainFunction.getContent()
                ))
                .collect(Collectors.toList());

        List<TechStackData> techStackDataList = foundTechStacks.stream()
                .map(techStack -> new TechStackData(
                        techStack.getTechStackId(),
                        techStack.getContent()
                ))
                .collect(Collectors.toList());

        IdeaInputRequest ideaInputRequest = new IdeaInputRequest(
                foundIdeaInput.getProjectName(),
                foundIdeaInput.getProjectTarget(),
                mainFunctionDataList,
                techStackDataList,
                foundIdeaInput.getProjectDescription()
        );

        String projectOverviewJson;
        String requirements;

        try {
            projectOverviewJson = objectMapper.writeValueAsString(ideaInputRequest);
            requirements = objectMapper.writeValueAsString(requests);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패: " + e.getMessage(), e);
        }

        // MLOps URL 설정
        String mlopsUrl = "http://3.34.185.3:8000/api/PJA/json_Summury/generate";

        ProjectInfoCreateRequest projectInfoCreateRequest = ProjectInfoCreateRequest.builder()
                .projectOverview(projectOverviewJson)
                .requirements(requirements)
                .build();

        try {
            ResponseEntity<ProjectInfoCreateResponse> response = restTemplate.postForEntity(
                    mlopsUrl,
                    projectInfoCreateRequest,
                    ProjectInfoCreateResponse.class);

            ProjectInfoCreateResponse body = response.getBody();
            ProjectInfoData projectInfoData = body.getJson().getProjectInfo();
            log.info("=== 프로젝트 정보 ml에서 받은거 : {}", projectInfoData);
            ProblemSolvingData problemSolvingData = projectInfoData.getProblemSolving();
            ProblemSolving converted = ProblemSolving.builder()
                    .currentProblem(problemSolvingData.getCurrentProblem())
                    .solutionIdea(problemSolvingData.getSolutionIdea())
                    .expectedBenefits(problemSolvingData.getExpectedBenefits())
                    .build();

            ProjectInfo savedProjectInfo = projectInfoRepository.save(
                    ProjectInfo.builder()
                            .workspace(foundWorkspace)
                            .title(projectInfoData.getTitle())
                            .category(projectInfoData.getCategory())
                            .targetUsers(projectInfoData.getTargetUsers())
                            .coreFeatures(projectInfoData.getCoreFeatures())
                            .technologyStack(projectInfoData.getTechnologyStack())
                            .problemSolving(converted)
                            .build()
            );

            foundWorkspace.updateProgressStep(ProgressStep.TWO);

            return new ProjectInfoResponse(
                    savedProjectInfo.getProjectInfoId(),
                    projectInfoData.getTitle(),
                    projectInfoData.getCategory(),
                    projectInfoData.getTargetUsers(),
                    projectInfoData.getCoreFeatures(),
                    projectInfoData.getTechnologyStack(),
                    converted
            );
        }
        catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException("MLOps API 호출 실패: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }
    }

    // 프로젝트 정보 수정
    @Transactional
    public ProjectInfoResponse updateProjectInfo(Users user, Long workspaceId, Long projectInfoId, ProjectInfoRequest request) {
        // 수정 권한 확인(멤버 or 오너)
        workspaceService.authorizeOwnerOrMemberOrThrow(user.getUserId(), workspaceId, "이 워크스페이스에 수정할 권한이 없습니다.");

        // 맞으면 수정 가능
        ProjectInfo foundProjectInfo = projectInfoRepository.findById(projectInfoId)
                .orElseThrow(() -> new NotFoundException("요청하신 아이디어 요약을 찾을 수 없습니다."));

        foundProjectInfo.update(
                request.getTitle(),
                request.getCategory(),
                request.getTargetUsers(),
                request.getCoreFeatures(),
                request.getTechnologyStack(),
                request.getProblemSolving()
                );

        sseService.notifyWorkspaceChange(workspaceId, "project-info");

        // 최근 활동 기록 추가
        workspaceActivityService.addWorkspaceActivity(user, workspaceId, ActivityTargetType.PROJECT_INFO, ActivityActionType.UPDATE);

        return new ProjectInfoResponse(
                projectInfoId,
                request.getTitle(),
                request.getCategory(),
                request.getTargetUsers(),
                request.getCoreFeatures(),
                request.getTechnologyStack(),
                request.getProblemSolving()
        );
    }
}
