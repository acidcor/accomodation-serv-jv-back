package mate.academy.accommodationbookingservice.dto.accomnodation.address;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequestDto {
    @NotBlank
    private String streetLineOne;
    @NotBlank
    private String streetLineTwo;
    @NotBlank
    private String postalCode;
    @NotBlank
    private String city;
    @NotBlank
    private String region;
    @NotBlank
    private String apartment;
    private double latitude;
    private double longitude;
}
