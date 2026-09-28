package com.example.vpoptimizer.exception;

import com.example.vpoptimizer.exception.ApiErrorCode;

/** Thrown when a referenced resource does not exist. */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ApiErrorCode code, String message) {
        super(code, message);
    }
}
