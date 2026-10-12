package mate.academy.accommodationbooking.service.dto.booking;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import mate.academy.accommodationbooking.service.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbooking.service.dto.user.UserResponseDto;
import mate.academy.accommodationbooking.service.model.BookingStatus;

@Getter
@Setter
public class BookingResponseDto {
    private Long id;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private AccommodationResponseDto accommodation;
    private UserResponseDto user;
    private BookingStatus status;
}
