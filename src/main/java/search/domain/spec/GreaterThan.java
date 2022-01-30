package search.domain.spec;

public class GreaterThan extends Specification {
    private final String field;
    private final Integer value;

    public GreaterThan(String field, Integer value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public String field() {
        return field;
    }

    @Override
    public Object value() {
        return value;
    }
}
