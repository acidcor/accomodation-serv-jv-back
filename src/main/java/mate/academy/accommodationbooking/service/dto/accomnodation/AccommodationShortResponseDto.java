package mate.academy.accommodationbooking.service.dto.accomnodation;

import java.math.BigDecimal;
import mate.academy.accommodationbooking.service.model.Accommodation;

public record AccommodationShortResponseDto(
        Long id,
        Accommodation.Type type,
        String size,
        BigDecimal dailyRate,
        Integer availability
) {
}
