package datart.core.common;

import datart.core.base.exception.BaseException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BeanUtilsTest {

    @Test
    void shouldRejectNullRequiredField() {
        SampleBean bean = new SampleBean(null, "yu-bi");

        BaseException exception = assertThrows(
                BaseException.class,
                () -> BeanUtils.requireNotNull(bean, "id")
        );

        assertEquals("field id can not be null", exception.getMessage());
    }

    @Test
    void shouldValidateJakartaConstraints() {
        SampleBean bean = new SampleBean("1", "");

        BaseException exception = assertThrows(
                BaseException.class,
                () -> BeanUtils.validate(bean)
        );

        assertEquals("name:name required", exception.getMessage());
    }

    @Test
    void shouldValidatePatternConstraint() {
        SampleBean bean = new SampleBean("invalid", "yu-bi");

        BaseException exception = assertThrows(
                BaseException.class,
                () -> BeanUtils.validate(bean)
        );

        assertEquals("id:id must be numeric", exception.getMessage());
    }

    @Test
    void shouldPassValidBean() {
        SampleBean bean = new SampleBean("1", "yu-bi");

        assertDoesNotThrow(() -> BeanUtils.validate(bean));
        assertDoesNotThrow(() -> BeanUtils.requireNotNull(bean, "id", "name"));
    }

    private record SampleBean(
            @Pattern(regexp = "[0-9]+", message = "id must be numeric") String id,
            @NotBlank(message = "name required") String name
    ) {
    }
}
