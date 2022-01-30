package search.domain.spec;

public class Equal extends Specification {
    private final String field;
    private final String value;

    public Equal(String field, String value) {
        this.field = field;
        this.value = value;
    }

    public String field() { return field; }

    public String value() { return value; }
}
