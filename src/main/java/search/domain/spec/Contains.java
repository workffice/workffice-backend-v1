package search.domain.spec;

public class Contains extends Specification {
    private final String        listField;
    private final Specification elementCondition;

    public Contains(String listField, Specification elementCondition) {
        this.listField = listField;
        this.elementCondition = elementCondition;
    }

    public String field() { return listField; }

    public Specification value() { return elementCondition; }
}
