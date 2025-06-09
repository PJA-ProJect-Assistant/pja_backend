package com.project.PJA.websocket.dto;

import com.project.PJA.idea.dto.ProblemSolving;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdeaUpdateMessage {
    private String type;
    private Long userId;
    private Long workspaceId;
    private String title;
    private String category;
    private List<String> targetUsers;
    private List<String> coreFeatures;
    private List<String> technologyStack;
    private ProblemSolving problemSolving;
}
