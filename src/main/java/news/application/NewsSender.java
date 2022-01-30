package news.application;

import backoffice.application.office_branch.OfficeBranchFinder;
import backoffice.domain.office_branch.OfficeBranchId;
import backoffice.domain.role.Access;
import backoffice.domain.role.Permission;
import backoffice.domain.role.Resource;
import io.vavr.control.Either;
import news.application.dto.NewsError;
import news.domain.News;
import news.domain.NewsRepository;
import news.domain.OfficeBranch;
import news.domain.OfficeBranchRepository;
import shared.domain.email.EmailSender;
import shared.domain.email.Message;
import shared.domain.email.template.OfficeHolderNewsTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import static news.application.dto.NewsError.DB_ERROR;
import static news.application.dto.NewsError.NEWS_FORBIDDEN;
import static news.application.dto.NewsError.NEWS_NOT_FOUND;

@Service
public class NewsSender {
    private final NewsRepository         newsRepo;
    private final OfficeBranchRepository officeBranchRepo;
    private final OfficeBranchFinder     officeBranchFinder;
    private final EmailSender            emailSender;

    public NewsSender(
            NewsRepository         newsRepo,
            OfficeBranchRepository officeBranchRepo,
            OfficeBranchFinder     officeBranchFinder,
            EmailSender            emailSender
    ) {
        this.newsRepo           = newsRepo;
        this.officeBranchRepo   = officeBranchRepo;
        this.officeBranchFinder = officeBranchFinder;
        this.emailSender        = emailSender;
    }

    private void sendNews(News news, List<String> recipients) {
        var messages = recipients
                .stream()
                .map(recipient -> Message
                        .builder()
                        .recipient(recipient)
                        .template(OfficeHolderNewsTemplate.of(
                                news.toResponse().getSubject(),
                                news.toResponse().getTitle(),
                                news.toResponse().getBody()
                        )).build())
                .collect(Collectors.toList());
        emailSender.sendBatch(messages);
    }

    public Either<NewsError, Void> sendNews(String id) {
        return newsRepo.findById(id)
                .toEither(NEWS_NOT_FOUND)
                .filterOrElse(
                        news -> officeBranchFinder.findWithAuthorization(
                                OfficeBranchId.fromString(news.officeBranchId()),
                                Permission.create(Access.WRITE, Resource.NEWS)
                        ).isRight(),
                        news -> NEWS_FORBIDDEN)
                .filterOrElse(News::isDraft, news -> NewsError.NEWS_IS_NOT_DRAFT)
                .map(news -> {
                    var recipients = officeBranchRepo
                            .findById(news.officeBranchId())
                            .map(OfficeBranch::renterEmails)
                            .getOrElse(new ArrayList<>());
                    this.sendNews(news, recipients);
                    news.markAsSent(recipients);
                    return news;
                }).flatMap(newsUpdated -> newsRepo.update(newsUpdated).toEither(DB_ERROR));
    }
}
