package mate.academy.accommodationbookingservice.service.telegram;

import mate.academy.accommodationbookingservice.dto.booking.BookingResponseDto;

public interface NotificationService {
    void sendBookingCanceled(BookingResponseDto responseDto);

    void sendBookingExpired(BookingResponseDto responseDto);

    void sendBookingCreated(BookingResponseDto responseDto);

    void sendBookingReleased(BookingResponseDto responseDto);
}
