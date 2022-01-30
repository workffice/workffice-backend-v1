package news.application;

import backoffice.application.dto.office_branch.OfficeBranchError;
import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import backoffice.factories.OfficeBranchBuilder;
import io.vavr.control.Either;
import io.vavr.control.Try;
import news.application.dto.NewsInfo;
import news.application.dto.NewsResponse;
import news.domain.News;
import news.domain.NewsRepository;
import shared.application.UseCaseError;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestNewsCreator {
    NewsRepository newsRepo = mock(NewsRepository.class);
    OfficeBranchFinder officeBranchFinder = mock(OfficeBranchFinder.class);
    ArgumentCaptor<News> newsArgumentCaptor = ArgumentCaptor.forClass(News.class);

    NewsCreator creator = new NewsCreator(newsRepo, officeBranchFinder);

    @Test
    void itShouldReturnNotFoundWhenOfficeBranchDoesNotExist() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.WRITE, Resource.NEWS)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST));
        var info = NewsInfo.of("Subject", "Title", "Body");

        Either<UseCaseError, Void> response = creator.create("1", officeBranchId.toString(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST);
    }

    @Test
    void itShouldReturnForbiddenWhenAuthUserDoesNotHaveAccessToNews() {
        var officeBranchId = new OfficeBranchId();
        when(officeBranchFinder.findWithAuthorization(
                officeBranchId,
                Permission.create(Access.WRITE, Resource.NEWS)
        )).thenReturn(Either.left(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN));
        var info = NewsInfo.of("Subject", "Title", "Body");

        Either<UseCaseError, Void> response = creator.create("1", officeBranchId.toString(), info);

        assertThat(response.isLeft()).isTrue();
        assertThat(response.getLeft()).isEqualTo(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN);
    }

    @Test
    void itShouldStoreNewsWithInfoSpecified() {
        var officeBranch = new OfficeBranchBuilder().build();
        when(officeBranchFinder.findWithAuthorization(
                officeBranch.id(),
                Permission.create(Access.WRITE, Resource.NEWS)
        )).thenReturn(Either.right(officeBranch.toResponse()));
        when(newsRepo.store(any())).thenReturn(Try.success(null));
        var info = NewsInfo.of("Subject", "Title", "Body");

        Either<UseCaseError, Void> response = creator.create("1", officeBranch.id().toString(), info);

        assertThat(response.isRight()).isTrue();
        verify(newsRepo, times(1)).store(newsArgumentCaptor.capture());
        var newsStored = newsArgumentCaptor.getValue();
        assertThat(newsStored.toResponse()).isEqualTo(NewsResponse.of(
                "1",
                "Subject",
                "Title",
                "Body",
                LocalDate.now(Clock.systemUTC()),
                "DRAFT",
                null,
                new HashSet<>()
        ));
    }
}
