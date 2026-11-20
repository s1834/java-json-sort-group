package com.example.assignment.exception;

import com.example.assignment.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ErrorResponse> badRequest(BadRequestException ex, HttpServletRequest request) { return response(HttpStatus.BAD_REQUEST, ex.getMessage(), request); }
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> invalidRequest(Exception ex, HttpServletRequest request) { return response(HttpStatus.BAD_REQUEST, "Request contains invalid or missing values", request); }
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex, HttpServletRequest request) { return response(HttpStatus.NOT_FOUND, ex.getMessage(), request); }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> routeNotFound(NoResourceFoundException ex, HttpServletRequest request) { return response(HttpStatus.NOT_FOUND, "Endpoint not found", request); }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorResponse> database(DataAccessException ex, HttpServletRequest request) { return response(HttpStatus.INTERNAL_SERVER_ERROR, "A database error occurred", request); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception ex, HttpServletRequest request) {
        Throwable rootCause = ex;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) rootCause = rootCause.getCause();
        logger.error("Unexpected exception handling {} {}{}; exception='{}'; rootCauseType='{}'; rootCause='{}'",
                request.getMethod(), request.getRequestURI(),
                request.getQueryString() == null ? "" : "?" + request.getQueryString(),
                ex.getMessage(), rootCause.getClass().getName(), rootCause.getMessage(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }
    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message));
    }
}
