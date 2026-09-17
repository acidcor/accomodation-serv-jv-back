package mate.academy.accommodationbookingservice.dto.accomnodation;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import mate.academy.accommodationbookingservice.dto.accomnodation.amenity.AmenityRequestDto;
import mate.academy.accommodationbookingservice.model.Accommodation;
import mate.academy.accommodationbookingservice.model.Address;

@Getter
@Setter
public class AccommodationPatchRequestDto {
    private final static String NOT_EMPTY_PATTERN = ".*\\S.*";

    private Accommodation.Type type;

    private Address location;
    @Pattern(regexp = NOT_EMPTY_PATTERN)
    private String size;
    @Size(min = 1)
    private List<AmenityRequestDto> amenities;
    @Positive
    private BigDecimal dailyRate;
    @PositiveOrZero
    private Integer availability;
}
