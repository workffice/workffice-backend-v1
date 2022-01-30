package controller;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import controller.response.Error;
import controller.response.ErrorResponse;
import controller.response.SingleResponse;
import controller.response.body.CreatedBody;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import static java.lang.String.format;

public abstract class BaseController {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<Error> errors = ex.getBindingResult().getAllErrors().stream().map((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            return Error.invalid("INVALID_" + fieldName.toUpperCase(), errorMessage);
        }).collect(Collectors.toList());
        return new ErrorResponse(errors);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ErrorResponse handleMissingRequestParam(MissingServletRequestParameterException ex) {
        return invalid(
                format("INVALID_%s", ex.getParameterName().toUpperCase()),
                format("Request param %s is required", ex.getParameterName())
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MismatchedInputException.class)
    public ErrorResponse handleMissingRequestParam(MismatchedInputException ex) {
        return invalid(
                format("INVALID_%s", ex.getPath().get(0).getFieldName().toUpperCase()),
                "Input type is invalid"
        );
    }

    protected <T> SingleResponse<T> entityResponse(T body) {
        return new SingleResponse<>(body);
    }

    protected SingleResponse<CreatedBody> entityCreated(String uri) {
        return new SingleResponse<>(CreatedBody.create(uri));
    }

    protected ErrorResponse invalid(String error, String message) {
        return ErrorResponse.fromSingleError(Error.invalid(error, message));
    }

    protected ErrorResponse notFound(String error, String message) {
        return ErrorResponse.fromSingleError(Error.notFound(error, message));
    }

    protected ErrorResponse forbidden(String error, String message) {
        return ErrorResponse.fromSingleError(Error.forbidden(error, message));
    }
}
