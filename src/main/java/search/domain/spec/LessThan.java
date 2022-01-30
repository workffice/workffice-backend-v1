package search.domain.spec;

public class LessThan extends Specification {
    private final String field;
    private final Integer value;

    public LessThan(String field, Integer value) {
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
