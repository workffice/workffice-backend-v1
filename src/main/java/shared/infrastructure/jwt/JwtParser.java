package shared.infrastructure.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.vavr.control.Option;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtParser {
    
    @Value("${jwt.secret}")
    private String secret;
    
    public <T> Option<T> parseToken(JwtToken token, String key, Class<T> clazz) {
        io.jsonwebtoken.JwtParser parser = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build();
        try {
            var claims = parser.parseClaimsJws(token.getToken());
            return Option.of(claims.getBody().get(key, clazz));
        } catch (IllegalArgumentException | JwtException e) {
            return Option.none();
        }
    }
}
