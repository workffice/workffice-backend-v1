package controller;

import backoffice.application.dto.office_branch.OfficeBranchError;
import controller.response.DataResponse;
import news.application.NewsCreator;
import news.application.NewsDeleter;
import news.application.NewsFinder;
import news.application.NewsSender;
import news.application.NewsUpdater;
import news.application.dto.NewsError;
import news.application.dto.NewsInfo;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static java.lang.String.format;

@RestController
@RequestMapping
public class NewsController extends BaseController {
    ResponseEntity<DataResponse> officeBranchNotFound = ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(notFound("OFFICE_BRANCH_NOT_FOUND", "There is no office branch with id specified"));
    ResponseEntity<DataResponse> newsForbidden = ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(forbidden("NEWS_FORBIDDEN", "You don't have access to news"));
    ResponseEntity<DataResponse> newsNotFound = ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(notFound("NEWS_NOT_FOUND", "There is not news with id provided"));
    ResponseEntity<DataResponse> newsIsNotDraft = ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(invalid("NEWS_IS_NOT_DRAFT", "News is not in draft status"));
    @Autowired private NewsCreator creator;
    @Autowired private NewsUpdater updater;
    @Autowired private NewsDeleter deleter;
    @Autowired private NewsSender  sender;
    @Autowired private NewsFinder  finder;

    @PostMapping("/api/office_branches/{officeBranchId}/news/")
    public ResponseEntity<?> createNews(@PathVariable String officeBranchId, @RequestBody NewsInfo info) {
        var newsId = UUID.randomUUID().toString();
        return creator
                .create(newsId, officeBranchId, info)
                .map(v -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body((DataResponse) entityCreated(format("/api/news/%s/", newsId))))
                .getOrElseGet(error -> Match(error).of(
                        Case($(OfficeBranchError.OFFICE_BRANCH_NOT_EXIST), () -> officeBranchNotFound),
                        Case($(OfficeBranchError.OFFICE_BRANCH_FORBIDDEN), () -> newsForbidden)
                ));
    }

    @GetMapping("/api/office_branches/{officeBranchId}/news/")
    public ResponseEntity<?> getNews(@PathVariable String officeBranchId) {
        return ResponseEntity.ok(entityResponse(finder.findByOfficeBranch(officeBranchId)));
    }

    @PutMapping("/api/news/{id}/")
    public ResponseEntity<?> updateNews(@PathVariable String id, @RequestBody NewsInfo info) {
        return updater.update(id, info)
                .map(v -> ResponseEntity.accepted().body((DataResponse) entityResponse("success :'(")))
                .getOrElseGet(error -> Match(error).of(
                        Case($(NewsError.NEWS_IS_NOT_DRAFT), () -> newsIsNotDraft),
                        Case($(NewsError.NEWS_NOT_FOUND), () -> newsNotFound),
                        Case($(NewsError.NEWS_FORBIDDEN), () -> newsForbidden)
                ));
    }

    @DeleteMapping("/api/news/{id}/")
    public ResponseEntity<?> deleteNews(@PathVariable String id) {
        return deleter.delete(id)
                .map(v -> ResponseEntity.accepted().body((DataResponse) entityResponse("success :'(")))
                .getOrElseGet(error -> Match(error).of(
                        Case($(NewsError.NEWS_IS_NOT_DRAFT), () -> newsIsNotDraft),
                        Case($(NewsError.NEWS_NOT_FOUND), () -> newsNotFound),
                        Case($(NewsError.NEWS_FORBIDDEN), () -> newsForbidden)
                ));
    }

    @PostMapping("/api/news/{id}/messages/")
    public ResponseEntity<?> sendNews(@PathVariable String id) {
        return sender.sendNews(id)
                .map(v -> ResponseEntity.accepted().body((DataResponse) entityResponse("success :'(")))
                .getOrElseGet(error -> Match(error).of(
                        Case($(NewsError.NEWS_IS_NOT_DRAFT), () -> newsIsNotDraft),
                        Case($(NewsError.NEWS_NOT_FOUND), () -> newsNotFound),
                        Case($(NewsError.NEWS_FORBIDDEN), () -> newsForbidden)
                ));
    }

}
