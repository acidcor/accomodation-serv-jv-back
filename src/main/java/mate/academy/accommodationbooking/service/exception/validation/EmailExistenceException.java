package mate.academy.accommodationbooking.service.exception.validation;

public class EmailExistenceException extends ValidationException {
    public EmailExistenceException(String message) {
        super(message);
    }
}
