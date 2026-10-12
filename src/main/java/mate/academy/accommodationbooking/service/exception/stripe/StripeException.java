package mate.academy.accommodationbooking.service.exception.stripe;

public class StripeException extends RuntimeException {
    public StripeException(String message) {
        super(message);
    }
}
