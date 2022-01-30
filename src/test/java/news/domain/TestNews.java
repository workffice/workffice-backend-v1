package news.domain;

import com.google.inject.internal.util.ImmutableList;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestNews {

    @Test
    void itShouldCreateNewsWithDraftStatus() {
        var news = News.create("1", "12", "Test", "Bocagon", "Ex club");

        assertThat(news.toResponse().getStatus()).isEqualTo(NewsStatus.DRAFT.name());
    }

    @Test
    void itShouldMarkNewsAsSent() {
        var news = News.create("1", "12", "Test", "Bocagon", "Ex club");

        news.markAsSent(ImmutableList.of("pepito@mail.com", "napoleon@mail.com"));

        assertThat(news.toResponse().getStatus()).isEqualTo(NewsStatus.SENT.name());
        assertThat(news.toResponse().getRecipients()).containsExactly(
                "pepito@mail.com",
                "napoleon@mail.com"
        );
    }
}
