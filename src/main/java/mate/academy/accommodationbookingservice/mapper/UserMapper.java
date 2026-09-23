package mate.academy.accommodationbookingservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import mate.academy.accommodationbookingservice.config.MapperConfig;
import mate.academy.accommodationbookingservice.dto.user.UserResponseDto;
import mate.academy.accommodationbookingservice.dto.user.auth.UserRegisterRequestDto;
import mate.academy.accommodationbookingservice.dto.user.crud.UserPatchRequestDto;
import mate.academy.accommodationbookingservice.dto.user.crud.UserUpdateRequestDto;
import mate.academy.accommodationbookingservice.model.User;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    UserResponseDto toDto(User user);

    User toEntity(UserRegisterRequestDto request);

    @Mapping(target = "password", ignore = true)
    void updateUser(UserUpdateRequestDto request, @MappingTarget User user);

    @Mapping(target = "password", ignore = true)
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    void patchUser(UserPatchRequestDto request, @MappingTarget User user);
}
