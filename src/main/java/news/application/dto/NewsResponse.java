package news.application.dto;

import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Value(staticConstructor = "of")
public class NewsResponse {
    String        id;
    String        subject;
    String        title;
    String        body;
    LocalDate     created;
    String        status;
    LocalDateTime sentAt;
    Set<String>   recipients;
}
