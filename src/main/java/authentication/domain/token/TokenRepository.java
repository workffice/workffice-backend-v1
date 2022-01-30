package authentication.domain.token;

import io.vavr.control.Option;

public interface TokenRepository {
    void store(Token token);
    
    Option<Token> find(String token);
}
