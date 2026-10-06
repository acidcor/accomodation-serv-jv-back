package mate.academy.accommodationbookingservice.exception.stripe;

public class StripeEventException extends StripeException {
    public StripeEventException(String message) {
        super(message);
    }
}
