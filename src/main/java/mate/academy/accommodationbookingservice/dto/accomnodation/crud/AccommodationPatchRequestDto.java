package mate.academy.accommodationbookingservice.dto.accomnodation.crud;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import mate.academy.accommodationbookingservice.dto.accomnodation.address.AddressRequestDto;
import mate.academy.accommodationbookingservice.model.Accommodation;

@Getter
@Setter
public class AccommodationPatchRequestDto {
    private final static String NOT_EMPTY_PATTERN = ".*\\S.*";

    private Accommodation.Type type;
    @Valid
    private AddressRequestDto location;
    @Pattern(regexp = NOT_EMPTY_PATTERN)
    private String size;
    private List<Long> amenities;
    @Positive
    private BigDecimal dailyRate;
    @PositiveOrZero
    private Integer availability;
}
