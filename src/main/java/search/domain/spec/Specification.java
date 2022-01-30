package search.domain.spec;

public abstract class Specification {
    private Integer offset = 0;
    private Integer limit = 20;

    public static Specification anyMatch(String field, Specification spec) {
        return new Contains(field, spec);
    }

    public static Specification eq(String field, String value) {
        return new Equal(field, value);
    }

    public static Specification gt(String field, Integer value) { return new GreaterThan(field, value); }

    public static Specification lt(String field, Integer value) { return new LessThan(field, value); }

    public static Specification between(String field, Integer greaterThan, Integer lessThan) {
        return new Between(field, greaterThan, lessThan);
    }

    public abstract String field();

    public abstract Object value();

    public Integer offset() { return offset; }

    public Integer limit() { return limit; }

    public Specification range(Integer offset, Integer limit) {
        this.offset = offset;
        this.limit = limit;
        return this;
    }
}
