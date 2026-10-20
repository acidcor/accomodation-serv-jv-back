package mate.academy.accommodationbooking.service.mapper;

import org.mapstruct.Mapper;
import mate.academy.accommodationbooking.service.config.MapperConfig;
import mate.academy.accommodationbooking.service.dto.telegram.TelegramChatRequestDto;
import mate.academy.accommodationbooking.service.dto.telegram.TelegramChatResponseDto;
import mate.academy.accommodationbooking.service.model.TelegramChat;

@Mapper(config = MapperConfig.class)
public interface TelegramChatMapper {
    TelegramChatResponseDto toDto(TelegramChat entity);

    TelegramChat toEntity(TelegramChatRequestDto dto);
}
