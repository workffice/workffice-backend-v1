package controller.response;

import lombok.Getter;

@Getter
public class SingleResponse<T> extends DataResponse {

    private final T data;

    public SingleResponse(T data) {
        this.data = data;
    }
}
