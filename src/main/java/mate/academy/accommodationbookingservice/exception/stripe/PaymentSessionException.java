package mate.academy.accommodationbookingservice.exception.stripe;

public class PaymentSessionException extends StripeException {
    public PaymentSessionException(String message) {
        super(message);
    }
}
