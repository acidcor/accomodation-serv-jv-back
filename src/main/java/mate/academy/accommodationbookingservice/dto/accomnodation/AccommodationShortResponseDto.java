package mate.academy.accommodationbookingservice.dto.accomnodation;

import java.math.BigDecimal;
import mate.academy.accommodationbookingservice.model.Accommodation;

public record AccommodationShortResponseDto(
        Long id,
        Accommodation.Type type,
        String size,
        BigDecimal dailyRate,
        Integer availability
) {
}
