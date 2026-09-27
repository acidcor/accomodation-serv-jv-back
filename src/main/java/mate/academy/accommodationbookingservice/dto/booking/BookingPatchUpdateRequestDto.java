package mate.academy.accommodationbookingservice.dto.booking;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingPatchUpdateRequestDto {
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Long accommodation;
}
