package mate.academy.accommodationbookingservice.exception;

public class TelegramChatNotFoundException extends RuntimeException {
    public TelegramChatNotFoundException(String message) {
        super(message);
    }
}
