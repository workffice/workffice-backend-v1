package authentication.infrastructure.springsecurity;

import authentication.domain.token.Token;
import authentication.domain.token.TokenGenerator;
import authentication.domain.user.AuthUser;
import authentication.domain.user.AuthUserId;
import io.jsonwebtoken.JwtException;
import io.vavr.control.Option;
import shared.infrastructure.jwt.JwtGenerator;
import shared.infrastructure.jwt.JwtParser;
import shared.infrastructure.jwt.JwtToken;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenGenerator implements TokenGenerator {
    
    private final JwtGenerator generator;
    private final JwtParser parser;
    
    public JwtTokenGenerator(JwtGenerator generator, JwtParser parser) {
        this.generator = generator;
        this.parser = parser;
    }
    
    @Override
    public Token create(AuthUser authUser) {
        Map<String, String> map = new HashMap<>() {{
            put("subject", authUser.email());
            put("auth_user_id", authUser.id().toString());
        }};
        String token = generator.generate(map).getOrElse(JwtToken.of("")).getToken();
        return new Token(token);
    }
    
    @Override
    public Option<AuthUserId> parseToken(Token token) {
        try {
            Option<String> authUserId = parser.parseToken(
                    JwtToken.of(token.token()),
                    "auth_user_id",
                    String.class
            );
            return authUserId.map(id -> new AuthUserId(UUID.fromString(id)));
        } catch (IllegalArgumentException | JwtException e) {
            return Option.none();
        }
    }
}
