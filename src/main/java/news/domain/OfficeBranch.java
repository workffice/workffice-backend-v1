package news.domain;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document(collection = "news__office_branches")
@NoArgsConstructor
@EqualsAndHashCode
public class OfficeBranch {
    private static final Integer RENTER_EMAIL_SIZE = 30;
    @MongoId
    private String id;
    private List<String> lastRenterEmails;

    public OfficeBranch(String id) {
        this.id = id;
        this.lastRenterEmails = new ArrayList<>();
    }

    public void addRenterEmail(String renterEmail) {
        if (lastRenterEmails.contains(renterEmail))
            return;
        lastRenterEmails.add(renterEmail);
        if (lastRenterEmails.size() > RENTER_EMAIL_SIZE)
            lastRenterEmails.remove(0);
    }

    public List<String> renterEmails() { return lastRenterEmails; }
}
