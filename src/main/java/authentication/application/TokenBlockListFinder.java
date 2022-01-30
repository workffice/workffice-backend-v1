package authentication.application;

import authentication.domain.token.Token;
import authentication.domain.token.TokenRepository;
import io.vavr.control.Option;

import org.springframework.stereotype.Service;

@Service
public class TokenBlockListFinder {
    
    private final TokenRepository repo;
    
    public TokenBlockListFinder(TokenRepository repo) {
        this.repo = repo;
    }
    
    public Option<Token> findToken(String token) {
        return repo.find(token);
    }
}
