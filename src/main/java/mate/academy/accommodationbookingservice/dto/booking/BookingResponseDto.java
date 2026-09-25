package mate.academy.accommodationbookingservice.dto.booking;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbookingservice.dto.user.UserResponseDto;
import mate.academy.accommodationbookingservice.model.BookingStatus;

@Getter
@Setter
public class BookingResponseDto {
    private Long id;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private AccommodationResponseDto accommodation;
    private UserResponseDto user;
    private BookingStatus bookingStatus;
}
