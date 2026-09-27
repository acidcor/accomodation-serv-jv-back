package mate.academy.accommodationbookingservice.dto.booking;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingRequestDto {
    @NotNull
    private LocalDate checkIn;
    @NotNull
    private LocalDate checkOut;
    @NotNull
    private Long accommodation;
}
