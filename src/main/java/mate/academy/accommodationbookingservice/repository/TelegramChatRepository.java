package mate.academy.accommodationbookingservice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import mate.academy.accommodationbookingservice.model.TelegramChat;

public interface TelegramChatRepository extends JpaRepository<TelegramChat, Long> {
    List<TelegramChat> findTelegramChatsByIsSubscribed(boolean isSubscribed);

    void deleteByChatId(Long chatId);

    boolean existsTelegramChatByChatId(Long chatId);
}
