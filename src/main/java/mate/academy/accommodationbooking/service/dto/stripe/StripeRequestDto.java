package mate.academy.accommodationbooking.service.dto.stripe;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StripeRequestDto {
    @NotNull
    private String id;
    @NotNull
    private String status;
}
