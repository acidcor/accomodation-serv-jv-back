package mate.academy.accommodationbookingservice.service.telegram;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mate.academy.accommodationbookingservice.dto.booking.BookingResponseDto;
import mate.academy.accommodationbookingservice.dto.telegram.TelegramChatRequestDto;
import mate.academy.accommodationbookingservice.model.TelegramChat;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationService implements NotificationService {
    private static final int BOT_BLOCKED_ERROR_CODE = 403;
    private static final String ACTION_EXPIRED = "expired";
    private static final String ACTION_CREATED = "created";
    private static final String ACTION_RELEASED = "released";
    private static final String ACTION_CANCELED = "canceled";
    private final TelegramChatService telegramChatService;

    @Value("${sk.telegram}")
    private String botToken;

    private TelegramBot bot;

    @PostConstruct
    private void initBot() {
        this.bot = new TelegramBot(botToken);
        this.bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                try {
                    addChatToDb(update);
                } catch (Exception e) {
                    // #TODO Add logger
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void addChatToDb(Update update) {
        if (update.message() == null || update.message().chat() == null) {
            return;
        }
        Long chatId = update.message().chat().id();
        if (checkChat(chatId)) {
            return;
        }
        TelegramChatRequestDto requestDto = new TelegramChatRequestDto();
        requestDto.setChatId(chatId);
        telegramChatService.addNewChat(requestDto);
    }

    @Override
    public void sendBookingCanceled(BookingResponseDto responseDto) {
        String message = createMessage(responseDto, ACTION_CANCELED);
        sendMessage(message);
    }

    @Override
    public void sendBookingExpired(BookingResponseDto responseDto) {
        String message = createMessage(responseDto, ACTION_EXPIRED);
        sendMessage(message);
    }

    @Override
    public void sendBookingCreated(BookingResponseDto responseDto) {
        String message = createMessage(responseDto, ACTION_CREATED);
        sendMessage(message);
    }

    @Override
    public void sendBookingReleased(BookingResponseDto responseDto) {
        String message = createMessage(responseDto, ACTION_RELEASED);
        sendMessage(message);
    }

    private boolean checkChat(Long chatId) {
        return telegramChatService.existsTelegramChatByChatId(chatId);
    }

    private String createMessage(BookingResponseDto responseDto, String action) {
        return String.format(
                """
                Booking was %s
                Booking ID: %s
                Check-in: %s
                Check-out: %s
                Status: %s
                """,
                action,
                responseDto.getId(),
                responseDto.getCheckIn(),
                responseDto.getCheckOut(),
                responseDto.getStatus()
        );
    }

    private void sendMessage(String message) {
        List<Long> subscribed = telegramChatService.getSubscribedInner()
                .stream()
                .map(TelegramChat::getChatId)
                .toList();
        for (Long chatId : subscribed) {
            SendResponse response = bot.execute(new SendMessage(chatId, message));
            if (!response.isOk()) {
                if (response.errorCode() == BOT_BLOCKED_ERROR_CODE) {
                    telegramChatService.deleteByChatId(chatId);
                }
            }
        }
    }
}
