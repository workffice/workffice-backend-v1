package news.application;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBranchBuilder;
import com.google.inject.internal.util.ImmutableList;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import news.application.dto.NewsError;
import news.domain.News;
import news.domain.NewsRepository;
import news.domain.OfficeBranch;
import news.domain.OfficeBranchRepository;
import news.factory.NewsBuilder;
import shared.domain.email.EmailSender;
import shared.domain.email.Message;
import shared.domain.email.template.OfficeHolderNewsTemplate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestNewsSender {
    NewsRepository newsRepo = mock(NewsRepository.class);
    OfficeBranchRepository officeBranchRepo = mock(OfficeBranchRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    EmailSender emailSender = mock(EmailSender.class);
    ArgumentCaptor<News> newsArgumentCaptor = ArgumentCaptor.forClass(News.class);

    NewsSender newsSender = new NewsSender(
            newsRepo,
            officeBranchRepo,
            officeBranchFinder,
            emailSender
    );

    @Test
    void itShouldReturnNotFoundWhenThereIsNoNewsWithIdProvided() {
        when(newsRepo.findById("1")).thenReturn(Option.none());

        Either<NewsError, Void> response = newsSender.sendNews("1");

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(NewsError.NEWS_NOT_FOUND);
    }

    @Test
    void itShouldReturnNewsForbiddenWhenAuthUserDoesNotHavePermission() {
        var news = new NewsBuilder().build();
        when(newsRepo.findById(news.id())).thenReturn(Option.of(news));
        when(officeBranchFinder.findWithAuthorization(
                any(),
                eq(Permission.create(Access.WRITE, Resource.NEWS))
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));

        Either<NewsError, Void> response = newsSender.sendNews(news.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(NewsError.NEWS_FORBIDDEN);
    }

    @Test
    void itShouldReturnNewsIsNotDraftWhenTryingToSendANewsThatIsDeleted() {
        var news = new NewsBuilder().build();
        news.delete();
        var officeBranch = new OfficeBranchBuilder().build();
        when(newsRepo.findById(news.id())).thenReturn(Option.of(news));
        when(officeBranchFinder.findWithAuthorization(
                any(),
                eq(Permission.create(Access.WRITE, Resource.NEWS))
        )).thenReturn(Either.right(officeBranch.toResponse()));

        Either<NewsError, Void> response = newsSender.sendNews(news.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(NewsError.NEWS_IS_NOT_DRAFT);
    }

    @Test
    void itShouldReturnNewsIsNotDraftWhenTryingToSendANewsThatIsSent() {
        var news = new NewsBuilder().build();
        news.markAsSent(ImmutableList.of("napoleon@mail.com"));
        var officeBranch = new OfficeBranchBuilder().build();
        when(newsRepo.findById(news.id())).thenReturn(Option.of(news));
        when(officeBranchFinder.findWithAuthorization(
                any(),
                eq(Permission.create(Access.WRITE, Resource.NEWS))
        )).thenReturn(Either.right(officeBranch.toResponse()));

        Either<NewsError, Void> response = newsSender.sendNews(news.id());

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(NewsError.NEWS_IS_NOT_DRAFT);
    }

    @Test
    void itShouldMarkNewsAsSentAndSendMessageForEveryOfficeBranchRecipient() {
        var news = new NewsBuilder().build();
        var officeBranch = new OfficeBranchBuilder().build();
        when(newsRepo.findById(news.id())).thenReturn(Option.of(news));
        when(officeBranchFinder.findWithAuthorization(
                any(),
                eq(Permission.create(Access.WRITE, Resource.NEWS))
        )).thenReturn(Either.right(officeBranch.toResponse()));
        var newsOfficeBranch = new OfficeBranch(officeBranch.id().toString());
        newsOfficeBranch.addRenterEmail("napoleon@mail.com");
        newsOfficeBranch.addRenterEmail("munieco@mail.com");
        newsOfficeBranch.addRenterEmail("91218@mail.com");
        when(officeBranchRepo.findById(news.officeBranchId()))
                .thenReturn(Option.of(newsOfficeBranch));
        when(newsRepo.update(any())).thenReturn(Try.success(null));

        Either<NewsError, Void> response = newsSender.sendNews(news.id());

        assertThat(response.isRight()).isTrue();
        var template = OfficeHolderNewsTemplate.of(
                news.toResponse().getSubject(),
                news.toResponse().getTitle(),
                news.toResponse().getBody()
        );
        verify(emailSender, times(1)).sendBatch(ImmutableList.of(
                new Message("napoleon@mail.com", template),
                new Message("munieco@mail.com", template),
                new Message("91218@mail.com", template)
        ));
        verify(newsRepo, times(1)).update(newsArgumentCaptor.capture());
        var newsUpdated = newsArgumentCaptor.getValue();
        assertThat(newsUpdated.isDraft()).isFalse();
        assertThat(newsUpdated.toResponse().getRecipients()).containsExactlyInAnyOrder(
                "napoleon@mail.com",
                "munieco@mail.com",
                "91218@mail.com"
        );
    }
}
