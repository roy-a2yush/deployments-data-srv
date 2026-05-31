package com.zephyr.deployments_data_srv.exception;

import com.zephyr.deployments_data_srv.ListDeployments400Response;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ListDeployments400Response> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ListDeployments400Response error = new ListDeployments400Response();
        error.setType(URI.create("https://api.deployments.com/errors/not-found"));
        error.setTitle("Resource Not Found");
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setDetail(ex.getMessage());
        error.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ListDeployments400Response> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        ListDeployments400Response error = new ListDeployments400Response();
        error.setType(URI.create("https://api.deployments.com/errors/bad-request"));
        error.setTitle("Bad Request");
        error.setStatus(HttpStatus.BAD_REQUEST.value());
        error.setDetail(ex.getMessage());
        error.setInstance(URI.create(request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "")));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ListDeployments400Response> handleGeneralError(Exception ex, HttpServletRequest request) {
        ListDeployments400Response error = new ListDeployments400Response();
        error.setType(URI.create("https://api.deployments.com/errors/internal-server-error"));
        error.setTitle("Internal Server Error");
        error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.setDetail(ex.getMessage() != null ? ex.getMessage() : "An unexpected server error occurred.");
        error.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
