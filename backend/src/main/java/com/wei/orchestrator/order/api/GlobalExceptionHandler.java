package com.wei.orchestrator.order.api;

import com.wei.orchestrator.order.domain.exception.OrderAlreadyExistsException;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderAlreadyExistsException.class)
    public ProblemDetail handleOrderAlreadyExistsException(OrderAlreadyExistsException ex) {
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Order Already Exists");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }
}
