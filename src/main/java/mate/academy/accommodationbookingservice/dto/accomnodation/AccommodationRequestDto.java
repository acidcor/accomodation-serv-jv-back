package mate.academy.accommodationbookingservice.dto.accomnodation;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import mate.academy.accommodationbookingservice.dto.accomnodation.address.AddressRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.amenity.AmenityRequestDto;
import mate.academy.accommodationbookingservice.model.Accommodation;

@Getter
@Setter
public class AccommodationRequestDto {
    @NotNull
    private Accommodation.Type type;
    @NotNull
    private AddressRequestDto location;
    @NotBlank
    private String size;
    private List<AmenityRequestDto> amenities;
    @Positive
    private BigDecimal dailyRate;
    @Positive
    private Integer availability;
}
