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

import org.springframework.stereotype.Service;

import static news.application.dto.NewsError.DB_ERROR;
import static news.application.dto.NewsError.NEWS_FORBIDDEN;
import static news.application.dto.NewsError.NEWS_NOT_FOUND;

@Service
public class NewsUpdater {
    private final NewsRepository newsRepo;
    private final OfficeBranchFinder officeBranchFinder;

    public NewsUpdater(NewsRepository newsRepo, OfficeBranchFinder officeBranchFinder) {
        this.newsRepo = newsRepo;
        this.officeBranchFinder = officeBranchFinder;
    }

    public Either<NewsError, Void> update(String id, NewsInfo info) {
        return newsRepo.findById(id)
                .toEither(NEWS_NOT_FOUND)
                .filterOrElse(
                        news -> officeBranchFinder.findWithAuthorization(
                                OfficeBranchId.fromString(news.officeBranchId()),
                                Permission.create(Access.WRITE, Resource.NEWS)
                        ).isRight(),
                        news -> NEWS_FORBIDDEN)
                .filterOrElse(News::isDraft, n -> NewsError.NEWS_IS_NOT_DRAFT)
                .map(news -> news.update(info.getSubject(), info.getTitle(), info.getBody()))
                .flatMap(newsUpdated -> newsRepo.update(newsUpdated).toEither(DB_ERROR));
    }
}
