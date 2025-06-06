package com.project.PJA.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse extends Throwable {
    private String status;
    private String message;
}
