package mate.academy.accommodationbookingservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.handler.StripeWebHookHandler;

@RestController
@RequestMapping("/webhook/stripe")
@RequiredArgsConstructor
public class StripeWebHook {
    private final StripeWebHookHandler stripeWebHookHandler;

    @PostMapping
    public void handler(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        stripeWebHookHandler.handle(payload, signature);
    }
}
