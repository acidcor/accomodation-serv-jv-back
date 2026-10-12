package mate.academy.accommodationbooking.service.dto.accomnodation;

import java.math.BigDecimal;
import java.util.List;
import mate.academy.accommodationbooking.service.dto.accomnodation.address.AddressResponseDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.amenity.AmenityResponseDto;
import mate.academy.accommodationbooking.service.model.Accommodation;

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
