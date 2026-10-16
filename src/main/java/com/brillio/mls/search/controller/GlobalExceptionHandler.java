package com.brillio.mls.search.controller;

import java.util.List;

import com.brillio.mls.search.dto.ApiError;
import com.brillio.mls.search.exception.InvalidSearchCriteriaException;
import com.brillio.mls.search.exception.ListingNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler
{
    @ExceptionHandler(InvalidSearchCriteriaException.class)
    public ResponseEntity<ApiError> handleInvalidCriteria(InvalidSearchCriteriaException ex,
                                                          HttpServletRequest request)
    {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex.getErrors());
    }

    @ExceptionHandler(ListingNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ListingNotFoundException ex,
                                                   HttpServletRequest request)
    {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, ex.getAvailableOptions());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request)
    {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName()
                + "'; expected type " + ex.getRequiredType().getSimpleName();
        return build(HttpStatus.BAD_REQUEST, message, request, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex,
                                                           HttpServletRequest request)
    {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest request, List<String> details)
    {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
