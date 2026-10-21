package dev.lizarazu.profileapi.profile;

import java.net.URI;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice(assignableTypes = BusinessProfileController.class)
public class BusinessProfileExceptionHandler {

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleValidation(
            HandlerMethodValidationException exception
    ) {
        String detail = exception.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(message -> message != null)
                .findFirst()
                .orElse("Request validation failed");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(),
                detail
        );

        problem.setType(URI.create("about:blank"));
        problem.setTitle("Request validation failed");

        return problem;
    }
}