package com.wei.orchestrator.order.api;

import com.wei.orchestrator.order.domain.exception.InvalidOrderStatusException;
import com.wei.orchestrator.order.domain.exception.OrderAlreadyExistsException;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;
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

    @ExceptionHandler(InvalidOrderStatusException.class)
    public ProblemDetail handleInvalidOrderStatusException(InvalidOrderStatusException ex) {
        String validValues =
                Arrays.stream(OrderStatus.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));

        String errorDetail =
                String.format(
                        "Invalid status value: '%s'. Valid values are: %s",
                        ex.getInvalidValue(), validValues);

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errorDetail);
        problemDetail.setTitle("Invalid Parameter Value");
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }
}
