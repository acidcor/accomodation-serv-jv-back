package mate.academy.accommodationbooking.service.dto.telegram;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TelegramChatResponseDto {
    Long id;
    Long chatId;
    boolean isSubscribed;
}
