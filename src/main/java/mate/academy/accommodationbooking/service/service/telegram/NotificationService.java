package mate.academy.accommodationbooking.service.service.telegram;

import mate.academy.accommodationbooking.service.dto.booking.BookingResponseDto;
import mate.academy.accommodationbooking.service.model.Payment;

public interface NotificationService {
    void sendBookingCanceled(BookingResponseDto responseDto);

    void sendBookingExpired(BookingResponseDto responseDto);

    void sendBookingCreated(BookingResponseDto responseDto);

    void sendBookingReleased(BookingResponseDto responseDto);

    void sendPaymentConfirmed(Payment confirmed);

    void sendPaymentExpired(Payment expired);
}
