package mate.academy.accommodationbooking.service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import mate.academy.accommodationbooking.service.model.TelegramChat;

public interface TelegramChatRepository extends JpaRepository<TelegramChat, Long> {
    List<TelegramChat> findTelegramChatsByIsSubscribed(boolean isSubscribed);

    void deleteByChatId(Long chatId);

    boolean existsTelegramChatByChatId(Long chatId);
}
