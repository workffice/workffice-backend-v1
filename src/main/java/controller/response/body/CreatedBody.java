package controller.response.body;

import lombok.Getter;

@Getter
public class CreatedBody {
    private final String uri;

    private CreatedBody(String uri) {
        this.uri = uri;
    }

    public static CreatedBody create(String uri) {
        return new CreatedBody(uri);
    }
}
