package mate.academy.accommodationbooking.service.dto.payment;

import java.math.BigDecimal;
import java.net.URI;
import mate.academy.accommodationbooking.service.dto.booking.BookingResponseDto;

public record PaymentResponseDto(
        Long id,
        String status,
        BookingResponseDto booking,
        URI sessionUrl,
        BigDecimal amount
) {
}
