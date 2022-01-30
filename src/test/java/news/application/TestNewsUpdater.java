package news.application;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBranchBuilder;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import news.application.dto.NewsError;
import news.application.dto.NewsInfo;
import news.domain.NewsRepository;
import news.factory.NewsBuilder;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestNewsUpdater {
    NewsRepository newsRepo = mock(NewsRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);

    NewsUpdater updater = new NewsUpdater(newsRepo, officeBranchFinder);

    @Test
    void itShouldReturnNotFoundWhenNewsDoesNotExist() {
        when(newsRepo.findById("1")).thenReturn(Option.none());
        var info = NewsInfo.of("Subject", "title", "body");

        Either<NewsError, Void> response = updater.update("1", info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(NewsError.NEWS_NOT_FOUND);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToNews() {
        var news = new NewsBuilder().build();
        when(newsRepo.findById(news.id())).thenReturn(Option.of(news));
        when(officeBranchFinder.findWithAuthorization(
                OfficeBranchId.fromString(news.officeBranchId()),
                Permission.create(Access.WRITE, Resource.NEWS)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));
        var info = NewsInfo.of("Subject", "title", "body");

        Either<NewsError, Void> response = updater.update(news.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(NewsError.NEWS_FORBIDDEN);
    }

    @Test
    void itShouldReturnNewsNotDraftWhenTryingToEditANonDraftNews() {
        var news = new NewsBuilder().build();
        news.delete();
        when(newsRepo.findById(news.id())).thenReturn(Option.of(news));
        when(officeBranchFinder.findWithAuthorization(
                OfficeBranchId.fromString(news.officeBranchId()),
                Permission.create(Access.WRITE, Resource.NEWS)
        )).thenReturn(Either.right(new OfficeBranchBuilder().build().toResponse()));
        when(newsRepo.update(any())).thenReturn(Try.success(null));
        var info = NewsInfo.of("Subject", "title", "body");

        Either<NewsError, Void> response = updater.update(news.id(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(NewsError.NEWS_IS_NOT_DRAFT);
    }

    @Test
    void itShouldCallUpdateWithNewInfoProvided() {
        var news = new NewsBuilder().build();
        when(newsRepo.findById(news.id())).thenReturn(Option.of(news));
        when(officeBranchFinder.findWithAuthorization(
                OfficeBranchId.fromString(news.officeBranchId()),
                Permission.create(Access.WRITE, Resource.NEWS)
        )).thenReturn(Either.right(new OfficeBranchBuilder().build().toResponse()));
        when(newsRepo.update(any())).thenReturn(Try.success(null));
        var info = NewsInfo.of("Subject", "title", "body");

        Either<NewsError, Void> response = updater.update(news.id(), info);

        assertThat(response.isRight()).isTrue();
        var newsUpdated = news.update(info.getSubject(), info.getTitle(), info.getBody());
        verify(newsRepo, times(1)).update(newsUpdated);
    }
}
