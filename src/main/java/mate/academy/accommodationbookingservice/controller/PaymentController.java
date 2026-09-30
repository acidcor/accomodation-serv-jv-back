package mate.academy.accommodationbookingservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.payment.PaymentRedirectionResponse;
import mate.academy.accommodationbookingservice.dto.payment.PaymentRequestDto;
import mate.academy.accommodationbookingservice.dto.payment.PaymentResponseDto;
import mate.academy.accommodationbookingservice.service.paymant.PaymentService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    @GetMapping("/")
    public Page<PaymentResponseDto> findAll(Authentication authentication, Pageable pageable) {
        return paymentService.finaAll(authentication, pageable);
    }

    @PreAuthorize("hasAuthority('CUSTOMER')")
    @PostMapping
    public PaymentRedirectionResponse initPayment(
            @RequestBody @Valid PaymentRequestDto request,
            Authentication authentication) {
        return paymentService.initPayment(request, authentication);
    }

    @PreAuthorize("hasAuthority('CUSTOMER')")
    @GetMapping("/success/")
    public PaymentResponseDto successPayment(
            Authentication authentication,
            @RequestParam(name = "session_id") String sessionId
    ) {
        return paymentService.getSuccess(authentication, sessionId);
    }

    @PreAuthorize("hasAuthority('CUSTOMER')")
    @GetMapping("/cancel/")
    public PaymentResponseDto cancelPayment(
            Authentication authentication,
            @RequestParam(name = "session_id") @RequestBody String sessionId
    ) {
        return paymentService.getCancel(authentication, sessionId);
    }

}
