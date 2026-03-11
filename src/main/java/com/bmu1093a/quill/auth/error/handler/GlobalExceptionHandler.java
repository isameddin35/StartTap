package com.bmu1093a.quill.auth.error.handler;

import com.bmu1093a.quill.auth.error.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException e, WebRequest request) {
        return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND, request);
    }

//    @ExceptionHandler(WrongPasswordException.class)
//    public ResponseEntity<Map<String, Object>> handleWrongPassword(WrongPasswordException e, WebRequest request) {
//        return createErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED, request);
//    }




    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("status", status.value());

        return new ResponseEntity<>(response, status);
    }

}
