package search.infrastructure;

import io.vavr.Tuple2;
import io.vavr.control.Try;
import search.domain.spec.Between;
import search.domain.spec.Contains;
import search.domain.spec.Equal;
import search.domain.spec.GreaterThan;
import search.domain.spec.LessThan;
import search.domain.spec.Specification;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import static com.google.common.base.Predicates.instanceOf;
import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

public class MongoCriteriaAdapter {

    public static Query obtainMongoQuery(Specification spec) {
        Criteria criteria = obtainMongoCriteria(spec);
        return Query.query(criteria).skip(spec.offset()).limit(spec.limit());
    }

    public static Try<Query> obtainMongoQuery(List<Specification> specs) {
        if (specs.isEmpty())
            return Try.failure(new IllegalArgumentException());
        Query query = new Query();
        var mongoCriterias = obtainMongoCriterias(specs);
        mongoCriterias.forEach(query::addCriteria);
        return Try.success(query);
    }

    public static Try<Query> obtainMongoQuery(List<Specification> specs, Integer offset, Integer limit) {
        if (specs.isEmpty())
            return Try.failure(new IllegalArgumentException());
        Query query = new Query();
        var mongoCriterias = obtainMongoCriterias(specs);
        mongoCriterias.forEach(query::addCriteria);
        return Try.success(query.skip(offset).limit(limit));
    }

    private static List<Criteria> obtainMongoCriterias(List<Specification> specs) {
        List<Specification> simpleSpecifications = specs.stream()
                .filter(spec -> spec.getClass() != Contains.class)
                .collect(Collectors.toList());
        List<Criteria> simpleMongoCriterias = obtainSimpleMongoCriterias(simpleSpecifications);
        Map<String, List<Specification>> nestedSpecsByField = specs
                .stream()
                .filter(spec -> spec.getClass() == Contains.class)
                .collect(Collectors.groupingBy(Specification::field));
        List<Criteria> nestedMongoCriterias = nestedSpecsByField
                .entrySet()
                .stream()
                .map(entry -> Criteria.where(entry.getKey()).elemMatch(obtainNestedMongoCriteria(entry.getValue())))
                .collect(Collectors.toList());
        simpleMongoCriterias.addAll(nestedMongoCriterias);
        return simpleMongoCriterias;
    }

    private static Criteria obtainNestedMongoCriteria(List<Specification> specs) {
        Function<List<Specification>, List<Specification>> obtainNestedCriteriasSkippingTheFirst =
                spec -> spec
                        .stream()
                        .skip(1)
                        .map(nestedSpec -> (Specification) nestedSpec.value())
                        .collect(Collectors.toList());
        var singleMongoCriteria = obtainMongoCriteria((Specification) specs.get(0).value());
        var multipleMongoCriteria = obtainMongoCriteria((Specification) specs.get(0).value()).andOperator(
                obtainSimpleMongoCriterias(obtainNestedCriteriasSkippingTheFirst.apply(specs))
                        .toArray(Criteria[]::new)
        );
        return specs.size() == 1 ? singleMongoCriteria : multipleMongoCriteria;
    }

    private static List<Criteria> obtainSimpleMongoCriterias(List<Specification> specs) {
        return specs.stream()
                .map(MongoCriteriaAdapter::obtainMongoCriteria)
                .collect(Collectors.toList());
    }

    private static Criteria obtainMongoCriteria(Specification spec) {
        return Match(spec).of(
                Case($(instanceOf(Equal.class)), eq -> Criteria
                        .where(eq.field()).regex(Pattern.compile(
                                "^" + eq.value().toString() + "$",
                                Pattern.CASE_INSENSITIVE
                        ))),
                Case($(instanceOf(Contains.class)), contains -> {
                    Specification elementCondition = (Specification) contains.value();
                    return Criteria
                            .where(contains.field())
                            .elemMatch(obtainMongoCriteria(elementCondition));
                }),
                Case($(instanceOf(GreaterThan.class)), greaterThan ->
                        Criteria.where(greaterThan.field()).gte(greaterThan.value())),
                Case($(instanceOf(LessThan.class)), lessThan ->
                        Criteria.where(lessThan.field()).lte(lessThan.value())),
                Case($(instanceOf(Between.class)), between ->
                {
                    Tuple2<Integer, Integer> values = (Tuple2) between.value();
                    return Criteria.where(between.field()).gte(values._1).lte(values._2);
                })
        );
    }
}
