package mate.academy.accommodationbooking.service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbooking.service.dto.payment.PaymentRedirectionResponse;
import mate.academy.accommodationbooking.service.dto.payment.PaymentRequestDto;
import mate.academy.accommodationbooking.service.dto.payment.PaymentResponseDto;
import mate.academy.accommodationbooking.service.service.payment.PaymentService;

@Tag(name = "Payments", description = "Endpoints for managing payments")
@RequiredArgsConstructor
@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(summary = "Get all payments",
            description = "Returns a paginated list of "
                    + "payments available to the authenticated user.")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    @GetMapping
    public Page<PaymentResponseDto> findAll(Authentication authentication, Pageable pageable) {
        return paymentService.findAll(authentication, pageable);
    }

    @Operation(summary = "Initialize a payment",
            description = "Creates a payment session and returns the payment redirection URL.")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @PostMapping
    public PaymentRedirectionResponse initPayment(
            @RequestBody @Valid PaymentRequestDto request,
            Authentication authentication) {
        return paymentService.initPayment(request, authentication);
    }

    @Operation(summary = "Renew a payment",
            description = "Creates a new payment session for an existing payment.")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @PostMapping("/{id}/")
    public PaymentRedirectionResponse renewPayment(
            @PathVariable Long id,
            Authentication authentication) {
        return paymentService.renewPayment(id, authentication);
    }

    @Operation(summary = "Synchronize a payment",
            description = "Synchronizes the payment status with the payment provider.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/{id}/sync")
    public void syncPayment(@PathVariable Long id) {
        paymentService.syncPayment(id);
    }

    @Operation(summary = "Synchronize all payments",
            description = "Synchronizes payment statuses with the payment provider.")
    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/all/sync")
    public void syncAllPayments() {
        paymentService.syncPayments();
    }

    @Operation(summary = "Get successful payment",
            description = "Returns payment details after successful payment processing.")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @GetMapping("/success")
    public PaymentResponseDto successPayment(
            Authentication authentication,
            @RequestParam(name = "session_id") String sessionId) {
        return paymentService.getSuccess(authentication, sessionId);
    }

    @Operation(summary = "Get canceled payment",
            description = "Returns payment details for a canceled payment session.")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @GetMapping("/cancel")
    public PaymentResponseDto cancelPayment(
            Authentication authentication,
            @RequestParam(name = "session_id") String sessionId) {
        return paymentService.getCancel(authentication, sessionId);
    }
}