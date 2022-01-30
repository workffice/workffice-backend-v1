package backoffice.factories;

import backoffice.domain.office.Office;
import backoffice.domain.office_inactivity.Inactivity;
import backoffice.domain.office_inactivity.InactivityId;
import backoffice.domain.office_inactivity.InactivityType;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class InactivityBuilder {
    private InactivityId id = new InactivityId();
    private InactivityType type = InactivityType.SPECIFIC_DATE;
    private DayOfWeek dayOfWeek = null;
    private LocalDate specificInactivityDay = LocalDate.now();
    private Office office = new OfficeBuilder().build();

    public InactivityBuilder withSpecificDate(LocalDate date) {
        this.type = InactivityType.SPECIFIC_DATE;
        this.specificInactivityDay = date;
        this.specificInactivityDay = null;
        return this;
    }

    public InactivityBuilder withDayOfWeek(DayOfWeek dayOfWeek) {
        this.type = InactivityType.RECURRING_DAY;
        this.dayOfWeek = dayOfWeek;
        this.specificInactivityDay = null;
        return this;
    }

    public InactivityBuilder withOffice(Office office) {
        this.office = office;
        return this;
    }

    public Inactivity build() {
        return new Inactivity(
                id,
                type,
                dayOfWeek,
                specificInactivityDay,
                office
        );
    }
}
