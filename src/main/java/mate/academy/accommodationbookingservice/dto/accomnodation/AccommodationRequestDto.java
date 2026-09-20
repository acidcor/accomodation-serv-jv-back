package mate.academy.accommodationbookingservice.dto.accomnodation;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import mate.academy.accommodationbookingservice.dto.accomnodation.address.AddressRequestDto;
import mate.academy.accommodationbookingservice.model.Accommodation;

@Getter
@Setter
public class AccommodationRequestDto {
    @NotNull
    private Accommodation.Type type;
    @NotNull
    @Valid
    private AddressRequestDto location;
    @NotBlank
    private String size;
    private List<Long> amenities;
    @Positive
    private BigDecimal dailyRate;
    @PositiveOrZero
    private Integer availability;
}
