package controller.validators;

import server.WorkfficeApplication;

import javax.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(classes = {WorkfficeApplication.class})
public class TestValueOfEnumValidator {
    @Autowired
    Validator validator;

    @Test
    void itShouldReturnEmptyViolationsWhenFieldIsEmpty() {
        var dtoExample = new DTOExample("");

        var violations = validator.validate(dtoExample);

        assertThat(violations).isEmpty();
    }

    @Test
    void itShouldReturnEmptyViolationsWhenFieldIsNull() {
        var dtoExample = new DTOExample(null);

        var violations = validator.validate(dtoExample);

        assertThat(violations).isEmpty();
    }

    @Test
    void itShouldReturnEmptyViolationWhenFieldValueIsPartOfEnum() {
        var dtoExample = new DTOExample("SOME");

        var violations = validator.validate(dtoExample);

        assertThat(violations).isEmpty();
    }

    @Test
    void itShouldReturnViolationWhenFieldIsNotPartOfEnum() {
        var dtoExample = new DTOExample("INVALID_VALUE");

        var violations = validator.validate(dtoExample);

        assertThat(violations).size().isEqualTo(1);
    }

    private enum EnumExample {
        SOME
    }

    private static class DTOExample {
        @ValueOfEnum(enumClass = EnumExample.class)
        String field;

        public DTOExample(String field) { this.field = field; }
    }
}
