package mate.academy.accommodationbookingservice.service.paymant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import mate.academy.accommodationbookingservice.dto.payment.PaymentRedirectionResponse;
import mate.academy.accommodationbookingservice.dto.payment.PaymentRequestDto;
import mate.academy.accommodationbookingservice.dto.payment.PaymentResponseDto;
import mate.academy.accommodationbookingservice.dto.stripe.StripeRequestDto;

public interface PaymentService {
    PaymentRedirectionResponse initPayment(
            PaymentRequestDto request, Authentication authentication);

    Page<PaymentResponseDto> finaAll(Authentication authentication, Pageable pageable);

    PaymentResponseDto getSuccess(Authentication authentication, String sessionId);

    PaymentResponseDto getCancel(Authentication authentication, String sessionId);

    void handleStripeCompleteRequest(StripeRequestDto requestDto);

    void handleStripeExpiredRequest(StripeRequestDto requestDto);
}
