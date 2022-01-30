package shared.domain;

import shared.domain.email.template.BookingPaymentAcceptedTemplate;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestBookingPaymentAcceptedTemplate {

    @Test
    void itShouldAddSubstitutionData() {
        var template = new BookingPaymentAcceptedTemplate(
                "http://localhost:3000",
                "123",
                "PIMBA",
                "2018-12-08",
                "Desde 19:00 hasta 20:00",
                5,
                (float) 500,
                "Mendoza",
                "Godoy Cruz",
                "5501",
                "Calle falsa 1234"
        );

        Map<String, String> substitutionData = template.substitutionData();

        assertThat(substitutionData.get("booking_url")).isEqualTo("http://localhost:3000/admin/booking?id=123");
        assertThat(substitutionData.get("booking_id")).isEqualTo("123");
        assertThat(substitutionData.get("office_name")).isEqualTo("PIMBA");
        assertThat(substitutionData.get("booking_schedule_date")).isEqualTo("2018-12-08");
        assertThat(substitutionData.get("booking_schedule_time")).isEqualTo("Desde 19:00 hasta 20:00");
        assertThat(substitutionData.get("price_per_hour")).isEqualTo("100.0");
        assertThat(substitutionData.get("booking_hours_quantity")).isEqualTo("5");
        assertThat(substitutionData.get("total_price")).isEqualTo("500.0");
        assertThat(substitutionData.get("office_branch_location"))
                .isEqualTo("Mendoza, Godoy Cruz 5501 - Calle falsa 1234");
    }
}
