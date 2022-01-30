package search.domain.spec;

import io.vavr.Tuple;

public class Between extends Specification {
    private final String  field;
    private final Integer greaterThan;
    private final Integer lessThan;

    public Between(String field, Integer greaterThan, Integer lessThan) {
        this.field       = field;
        this.greaterThan = greaterThan;
        this.lessThan    = lessThan;
    }

    @Override
    public String field() {
        return field;
    }

    @Override
    public Object value() {
        return Tuple.of(greaterThan, lessThan);
    }
}
