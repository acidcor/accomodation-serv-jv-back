package mate.academy.accommodationbooking.service.service.telegram;

import mate.academy.accommodationbooking.service.dto.booking.BookingResponseDto;

public interface NotificationService {
    void sendBookingCanceled(BookingResponseDto responseDto);

    void sendBookingExpired(BookingResponseDto responseDto);

    void sendBookingCreated(BookingResponseDto responseDto);

    void sendBookingReleased(BookingResponseDto responseDto);
}
