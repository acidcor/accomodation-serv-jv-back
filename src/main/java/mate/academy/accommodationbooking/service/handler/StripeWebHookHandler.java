package mate.academy.accommodationbooking.service.handler;

public interface StripeWebHookHandler {
    void handle(String payload, String signature);
}
