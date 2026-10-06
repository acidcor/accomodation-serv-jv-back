package mate.academy.accommodationbookingservice.exception.stripe;

public class StripeException extends RuntimeException {
    public StripeException(String message) {
        super(message);
    }
}
