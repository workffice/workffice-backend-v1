package authentication.infrastructure.repositories;

import authentication.domain.token.Token;
import authentication.domain.token.TokenRepository;
import io.vavr.control.Option;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TokenInMemoryRepo implements TokenRepository {
    private List<Token> tokens = new ArrayList<>();

    @Override
    public void store(Token token) {
        tokens.add(token);
    }
    
    @Override
    public Option<Token> find(String token) {
        return Option.ofOptional(tokens.stream().filter(t -> t.token().equals(token)).findFirst());
    }
}
