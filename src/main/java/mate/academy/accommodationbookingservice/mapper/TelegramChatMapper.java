package mate.academy.accommodationbookingservice.mapper;

import org.mapstruct.Mapper;
import mate.academy.accommodationbookingservice.config.MapperConfig;
import mate.academy.accommodationbookingservice.dto.telegram.TelegramChatRequestDto;
import mate.academy.accommodationbookingservice.dto.telegram.TelegramChatResponseDto;
import mate.academy.accommodationbookingservice.model.TelegramChat;

@Mapper(config = MapperConfig.class)
public interface TelegramChatMapper {
    TelegramChatResponseDto toDto(TelegramChat entity);

    TelegramChat toEntity(TelegramChatRequestDto dto);
}
