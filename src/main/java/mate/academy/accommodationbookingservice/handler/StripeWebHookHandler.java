package mate.academy.accommodationbookingservice.handler;

public interface StripeWebHookHandler {
    void handle(String payload, String signature);
}
