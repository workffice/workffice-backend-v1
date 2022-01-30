package shared;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.vavr.control.Option;
import server.WorkfficeApplication;
import shared.infrastructure.jwt.JwtGenerator;
import shared.infrastructure.jwt.JwtParser;
import shared.infrastructure.jwt.JwtToken;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestJwt {
    @Autowired
    JwtGenerator generator;
    @Autowired
    JwtParser parser;
    
    @Test
    public void itShouldReturnEmptyStringWhenTokenHasExpired() {
        io.jsonwebtoken.JwtParser jwtClientParser = mock(io.jsonwebtoken.JwtParser.class);
        when(jwtClientParser.parseClaimsJws("super_token")).thenThrow(ExpiredJwtException.class);
        
        Option<String> id = parser.parseToken(JwtToken.of("super_token"), "some", String.class);
        
        assertThat(id.isEmpty()).isTrue();
    }
    
    @Test
    public void itShouldReturnEmptyStringIdWhenTokenIsInvalid() {
        io.jsonwebtoken.JwtParser jwtClientParser = mock(io.jsonwebtoken.JwtParser.class);
        when(jwtClientParser.parseClaimsJws("super_token")).thenThrow(MalformedJwtException.class);
        
        Option<String> id = parser.parseToken(JwtToken.of("super_token"), "some", String.class);
        
        assertThat(id.isEmpty()).isTrue();
    }
    
    @Test
    public void itShouldReturnEmptyTokenWhenSubjectIsNotProvided() {
        Map<String, String> info = new HashMap<>() {{
            put("email_id", "1234");
        }};
        
        Option<JwtToken> token = generator.generate(info);
        
        assertThat(token.isEmpty()).isTrue();
    }
    
    @Test
    public void itShouldEncryptInformation() {
        Map<String, String> info = new HashMap<>() {{
            put("subject", "bla@mail.com");
            put("email_id", "1234");
        }};
        
        Option<JwtToken> token = generator.generate(info);
        Option<String> result = parser.parseToken(token.get(), "email_id", String.class);
        
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get()).isEqualTo("1234");
    }
}
