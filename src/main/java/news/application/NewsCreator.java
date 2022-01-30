package news.application;

import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;
import news.application.dto.NewsError;
import news.application.dto.NewsInfo;
import news.domain.News;
import news.domain.NewsRepository;
import shared.application.UseCaseError;

import org.springframework.stereotype.Service;

@Service
public class NewsCreator {
    private final NewsRepository     newsRepo;
    private final OfficeBranchFinder officeBranchFinder;

    public NewsCreator(NewsRepository newsRepo, OfficeBranchFinder officeBranchFinder) {
        this.newsRepo           = newsRepo;
        this.officeBranchFinder = officeBranchFinder;
    }

    public Either<UseCaseError, Void> create(
            String newsId,
            String officeBranchId,
            NewsInfo info
    ) {
        return officeBranchFinder.findWithAuthorization(
                OfficeBranchId.fromString(officeBranchId),
                Permission.create(Access.WRITE, Resource.NEWS))
                .map(officeBranchResponse -> News.create(
                        newsId,
                        officeBranchId,
                        info.getSubject(),
                        info.getTitle(),
                        info.getBody()))
                .flatMap(news -> newsRepo.store(news).toEither(NewsError.DB_ERROR));
    }
}
