package controller.response;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class ErrorResponse extends DataResponse {
    
    private final List<Error> errors;
    
    public ErrorResponse(List<Error> errors) {
        this.errors = errors;
    }
    
    public static ErrorResponse fromSingleError(Error error) {
        return new ErrorResponse(Collections.singletonList(error));
    }
}
