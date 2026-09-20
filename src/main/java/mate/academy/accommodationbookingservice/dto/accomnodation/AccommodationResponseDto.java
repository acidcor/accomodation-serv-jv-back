package mate.academy.accommodationbookingservice.dto.accomnodation;

import java.math.BigDecimal;
import java.util.List;
import mate.academy.accommodationbookingservice.dto.accomnodation.address.AddressResponseDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.amenity.AmenityResponseDto;
import mate.academy.accommodationbookingservice.model.Accommodation;

public record AccommodationResponseDto(
        Long id,
        Accommodation.Type type,
        AddressResponseDto location,
        String size,
        List<AmenityResponseDto> amenities,
        BigDecimal dailyRate,
        Integer availability
) {
}
