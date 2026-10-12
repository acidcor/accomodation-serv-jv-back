package mate.academy.accommodationbooking.service.exception.stripe;

public class PaymentSessionException extends StripeException {
    public PaymentSessionException(String message) {
        super(message);
    }
}
