package authentication.domain.token;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(of = {"token"})
public class Token {
    
    private final String token;
    
    public Token(String token) {
        this.token = token;
    }
    
    public String token() {
        return token;
    }
}
