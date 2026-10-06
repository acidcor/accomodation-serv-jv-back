package mate.academy.accommodationbookingservice.exception.notfound;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
