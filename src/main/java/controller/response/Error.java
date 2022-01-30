package controller.response;

import lombok.Getter;

@Getter
public class Error {

    private final String code;
    private final String error;
    private final String message;

    private Error(String code, String error, String message) {
        this.code = code;
        this.error = error;
        this.message = message;
    }

    public static Error invalid(String error, String message) {
        return new Error("INVALID", error, message);
    }

    public static Error notFound(String error, String message) {
        return new Error("NOT_FOUND", error, message);
    }

    public static Error forbidden(String error, String message) {
        return new Error("FORBIDDEN", error, message);
    }
}
