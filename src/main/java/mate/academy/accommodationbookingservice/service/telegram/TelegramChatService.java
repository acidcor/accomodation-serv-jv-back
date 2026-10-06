package mate.academy.accommodationbookingservice.service.telegram;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import mate.academy.accommodationbookingservice.dto.telegram.TelegramChatRequestDto;
import mate.academy.accommodationbookingservice.dto.telegram.TelegramChatResponseDto;
import mate.academy.accommodationbookingservice.model.TelegramChat;

public interface TelegramChatService {
    Page<TelegramChatResponseDto> getAll(Pageable pageable);

    TelegramChatResponseDto addNewChat(TelegramChatRequestDto request);

    List<TelegramChat> getSubscribedInner();

    TelegramChatResponseDto changeSubscriptionById(Long id);

    void delete(Long id);

    void deleteByChatId(Long id);

    boolean existsTelegramChatByChatId(Long chatId);
}
