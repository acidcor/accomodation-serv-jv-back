package mate.academy.accommodationbookingservice.service.telegram;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.telegram.TelegramChatRequestDto;
import mate.academy.accommodationbookingservice.dto.telegram.TelegramChatResponseDto;
import mate.academy.accommodationbookingservice.exception.notfound.EntityNotFoundException;
import mate.academy.accommodationbookingservice.mapper.TelegramChatMapper;
import mate.academy.accommodationbookingservice.model.TelegramChat;
import mate.academy.accommodationbookingservice.repository.TelegramChatRepository;

@RequiredArgsConstructor
@Service
@Transactional
public class TelegramChatServiceImpl implements TelegramChatService {
    private final TelegramChatMapper telegramChatMapper;
    private final TelegramChatRepository telegramChatRepository;

    @Override
    public Page<TelegramChatResponseDto> getAll(Pageable pageable) {
        return telegramChatRepository.findAll(pageable).map(telegramChatMapper::toDto);
    }

    @Override
    public TelegramChatResponseDto addNewChat(TelegramChatRequestDto request) {
        TelegramChat chat = telegramChatMapper.toEntity(request);
        return telegramChatMapper.toDto(telegramChatRepository.save(chat));
    }

    @Override
    public List<TelegramChat> getSubscribedInner() {
        return telegramChatRepository.findTelegramChatsByIsSubscribed(true).stream().collect(
                Collectors.toList());
    }

    @Override
    public TelegramChatResponseDto changeSubscriptionById(Long id) {
        TelegramChat chat = telegramChatRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Can't find telegram chat by Id: " + id)
        );
        chat.setSubscribed(!chat.isSubscribed());
        return telegramChatMapper.toDto(telegramChatRepository.save(chat));
    }

    @Override
    public void delete(Long id) {
        telegramChatRepository.deleteById(id);
    }

    @Override
    public void deleteByChatId(Long id) {
        telegramChatRepository.deleteByChatId(id);
    }

    @Override
    public boolean existsTelegramChatByChatId(Long chatId) {
        return telegramChatRepository.existsTelegramChatByChatId(chatId);
    }
}
