package mate.academy.accommodationbookingservice.mapper;

import org.mapstruct.Mapper;
import mate.academy.accommodationbookingservice.config.MapperConfig;
import mate.academy.accommodationbookingservice.dto.user.UserRegisterRequestDto;
import mate.academy.accommodationbookingservice.dto.user.UserResponseDto;
import mate.academy.accommodationbookingservice.model.User;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    User toEntityFromRegisterDto(UserRegisterRequestDto request);

    UserResponseDto toDto(User user);

    User toEntity(UserRegisterRequestDto request);
}
