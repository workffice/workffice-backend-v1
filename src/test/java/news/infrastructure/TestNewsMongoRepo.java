package news.infrastructure;

import com.google.inject.internal.util.ImmutableList;
import news.domain.News;
import server.WorkfficeApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = WorkfficeApplication.class)
public class TestNewsMongoRepo {

    @Autowired
    NewsMongoRepo newsRepo;

    @Test
    void itShouldStoreNews() {
        var news = News.create("1", "21", "Test", "Awesome new", "blabla");

        newsRepo.store(news);

        var newsStored = newsRepo.findById("1").get();
        assertThat(newsStored.toResponse()).isEqualTo(news.toResponse());
    }

    @Test
    void itShouldUpdateNews() {
        var news = News.create("1", "21", "Test", "Awesome new", "blabla");
        newsRepo.store(news);

        news.markAsSent(ImmutableList.of("napoleon@mail.com"));
        newsRepo.update(news);

        var newsUpdated = newsRepo.findById("1").get();
        assertThat(newsUpdated.toResponse()).isEqualTo(news.toResponse());
    }

    @Test
    void itShouldReturnAllNewsWithOfficeBranchIdSpecified() {
        var news = News.create("1", "21", "Test", "Awesome new", "blabla");
        var news2 = News.create("2", "21", "Test", "Awesome new", "blabla");
        var news3 = News.create("3", "21", "Test", "Awesome new", "blabla");
        newsRepo.store(news);
        newsRepo.store(news2);
        newsRepo.store(news3);

        var newsList = newsRepo.findByOfficeBranch("21");

        assertThat(newsList).size().isEqualTo(3);
    }
}
