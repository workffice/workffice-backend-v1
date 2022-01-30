package authentication.infrastructure;

import authentication.domain.token.Token;
import authentication.infrastructure.repositories.TokenInMemoryRepo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTokenInMemoryRepo {
    TokenInMemoryRepo tokenRepo = new TokenInMemoryRepo();
    
    @Test
    public void itShouldStoreToken() {
        tokenRepo.store(new Token("t12321"));
        
        assertThat(tokenRepo.find("t12321").get())
                .isEqualTo(new Token("t12321"));
    }
    
    @Test
    public void itShouldReturnEmptyWhenTokenIsNotStored() {
        assertThat(tokenRepo.find("nonexistent").isEmpty())
                .isTrue();
    }
}
