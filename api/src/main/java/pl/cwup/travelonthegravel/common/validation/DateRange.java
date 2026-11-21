package pl.cwup.travelonthegravel.common.validation;

import java.time.LocalDate;

public interface DateRange {

    LocalDate startDate();

    LocalDate endDate();
}
