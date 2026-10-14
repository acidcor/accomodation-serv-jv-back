package mate.academy.accommodationbooking.service.service.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import mate.academy.accommodationbooking.service.dto.payment.PaymentCancelingResponseDto;
import mate.academy.accommodationbooking.service.dto.payment.PaymentRedirectionResponse;
import mate.academy.accommodationbooking.service.dto.payment.PaymentRequestDto;
import mate.academy.accommodationbooking.service.dto.payment.PaymentResponseDto;
import mate.academy.accommodationbooking.service.dto.stripe.StripeRequestDto;

public interface PaymentService {
    PaymentRedirectionResponse initPayment(
            PaymentRequestDto request, Authentication authentication);

    Page<PaymentResponseDto> findAll(Authentication authentication, Pageable pageable);

    PaymentResponseDto getSuccess(Authentication authentication, String sessionId);

    PaymentCancelingResponseDto getCancel(Authentication authentication, String sessionId);

    void handleStripeCompleteRequest(StripeRequestDto requestDto);

    void handleStripeExpiredRequest(StripeRequestDto requestDto);

    void syncPayment(Long id);

    void syncPayments();

    PaymentRedirectionResponse renewPayment(Long request, Authentication authentication);
}
