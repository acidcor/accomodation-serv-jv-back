package mate.academy.accommodationbookingservice.exception.validation;

public class EmailExistenceException extends ValidationException {
    public EmailExistenceException(String message) {
        super(message);
    }
}
