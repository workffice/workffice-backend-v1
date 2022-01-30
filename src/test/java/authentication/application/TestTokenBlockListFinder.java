package authentication.application;

import authentication.domain.token.Token;
import authentication.domain.token.TokenRepository;
import io.vavr.control.Option;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestTokenBlockListFinder {
    TokenRepository tokenRepository = mock(TokenRepository.class);
    TokenBlockListFinder finder = new TokenBlockListFinder(tokenRepository);
    
    @Test
    public void itShouldReturnEmptyTokenWhenThereIsNotTokenSaved() {
        when(tokenRepository.find("nonexistent_token")).thenReturn(Option.none());
        
        assertThat(finder.findToken("nonexistent_token").isEmpty()).isTrue();
    }
    
    @Test
    public void itShouldReturnTokenSaved() {
        when(tokenRepository.find("super_token"))
                .thenReturn(Option.of(new Token("super_token")));
        
        assertThat(finder.findToken("super_token").get())
                .isEqualTo(new Token("super_token"));
    }
}
