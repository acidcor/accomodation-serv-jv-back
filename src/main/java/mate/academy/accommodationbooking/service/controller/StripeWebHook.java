package mate.academy.accommodationbooking.service.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbooking.service.handler.StripeWebHookHandler;

@Tag(name = "Stripe Webhook", description = "Endpoints for handling Stripe webhook events")
@RestController
@RequestMapping("/webhook/stripe")
@RequiredArgsConstructor
public class StripeWebHook {
    private final StripeWebHookHandler stripeWebHookHandler;

    @Operation(
            summary = "Handle Stripe webhook event",
            description = "Receives Stripe webhook events, validates the signature, "
                    + "and processes the event payload."
    )
    @PostMapping
    public void handler(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        stripeWebHookHandler.handle(payload, signature);
    }
}
