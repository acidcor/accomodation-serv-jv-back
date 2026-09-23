package mate.academy.accommodationbookingservice.exception;

public class EmailExistenceException extends RuntimeException {
    public EmailExistenceException(String message) {
        super(message);
    }
}
