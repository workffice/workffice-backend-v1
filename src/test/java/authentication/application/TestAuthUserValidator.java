package authentication.application;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAuthUserValidator {
    @Test
    void itShouldReturnFalseWhenEmailIsNotTheSameAsUserAuthenticated() {
        SecurityContextHolder
                .getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        new User("login@test.com", "", new ArrayList<>()),
                        ""
                ));
        AuthUserValidator validator = new AuthUserValidator();
        
        assertThat(validator.isSameUserAsAuthenticated("unknown@test.com")).isFalse();
    }
    
    @Test
    void itShouldReturnFalseWhenContextDoesNotHaveAuthentication() {
        SecurityContextHolder.clearContext();
        AuthUserValidator validator = new AuthUserValidator();
        
        assertThat(validator.isSameUserAsAuthenticated("unknown@test.com")).isFalse();
    }
    
    @Test
    void itShouldReturnTrueWhenEmailIsTheSameAsUserAuthenticated() {
        SecurityContextHolder
                .getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        new User("john@test.com", "", new ArrayList<>()),
                        ""
                ));
        AuthUserValidator validator = new AuthUserValidator();
        
        assertThat(validator.isSameUserAsAuthenticated("john@test.com")).isTrue();
    }
}
