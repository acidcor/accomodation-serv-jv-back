package mate.academy.accommodationbookingservice.dto.accomnodation.amenity;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AmenityRequestDto {
    @NotBlank
    private String name;
    private String description;
}
