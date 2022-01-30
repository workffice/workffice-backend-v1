package shared.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.vavr.control.Option;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtGenerator {
    
    @Value("${jwt.secret}")
    private String secret;
    
    public Option<JwtToken> generate(Map<String, String> info) {
        if (!info.containsKey("subject"))
            return Option.none();
        LocalDate now = LocalDate.now();
        LocalDate expiricyDate = now.plus(Period.ofDays(3));
        Claims claims = Jwts
                .claims()
                .setIssuedAt(Date.valueOf(now))
                .setSubject(info.get("subject"))
                .setExpiration(Date.valueOf(expiricyDate));
        info.forEach(claims::put);
        claims.put("id", UUID.randomUUID());
        String token = Jwts.builder()
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .setClaims(claims)
                .compact();
        return Option.of(JwtToken.of(token));
    }
}
