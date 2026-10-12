package mate.academy.accommodationbooking.service.service.telegram;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import mate.academy.accommodationbooking.service.dto.telegram.TelegramChatRequestDto;
import mate.academy.accommodationbooking.service.dto.telegram.TelegramChatResponseDto;
import mate.academy.accommodationbooking.service.model.TelegramChat;

public interface TelegramChatService {
    Page<TelegramChatResponseDto> getAll(Pageable pageable);

    TelegramChatResponseDto addNewChat(TelegramChatRequestDto request);

    List<TelegramChat> getSubscribedInner();

    TelegramChatResponseDto changeSubscriptionById(Long id);

    void delete(Long id);

    void deleteByChatId(Long id);

    boolean existsTelegramChatByChatId(Long chatId);
}
