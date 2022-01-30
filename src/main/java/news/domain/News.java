package news.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import news.application.dto.NewsResponse;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document(collection = "news")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class News {
    @MongoId
    private final String        id;
    private final String        officeBranchId;
    private final String        subject;
    private final String        title;
    private final String        body;
    private final LocalDate     created;
    private final Set<String>   recipients;
    private       NewsStatus    status;
    private       LocalDateTime sentAt;

    public static News create(
            String id,
            String officeBranchId,
            String subject,
            String title,
            String body
    ) {
        return new News(
                id,
                officeBranchId,
                subject,
                title,
                body,
                LocalDate.now(Clock.systemUTC()),
                new HashSet<>(),
                NewsStatus.DRAFT,
                null
        );
    }

    public News update(
            String subject,
            String title,
            String body
    ) {
        return new News(
                this.id,
                this.officeBranchId,
                subject,
                title,
                body,
                this.created,
                this.recipients,
                this.status,
                this.sentAt
        );
    }

    public String id() { return id; }

    public String officeBranchId() { return officeBranchId; }

    public boolean isDraft() { return status.equals(NewsStatus.DRAFT); }

    public void markAsSent(List<String> recipients) {
        this.recipients.addAll(recipients);
        this.status = NewsStatus.SENT;
        this.sentAt = LocalDateTime.now(Clock.systemUTC()).withSecond(0).withNano(0);
    }

    public void delete() {
        this.status = NewsStatus.DELETED;
    }

    public NewsResponse toResponse() {
        return NewsResponse.of(
                id,
                subject,
                title,
                body,
                created,
                status.name(),
                sentAt,
                recipients
        );
    }
}
