package backoffice.domain.office;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(of = {"url"})
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    private String url;

    public boolean hasEmptyInformation() {
        return url == null || url.isEmpty() || url.isBlank();
    }

    public String url() { return url; }
}
