package mate.academy.accommodationbookingservice.exception;

public class StripeEventException extends RuntimeException {
    public StripeEventException(String message) {
        super(message);
    }
}
