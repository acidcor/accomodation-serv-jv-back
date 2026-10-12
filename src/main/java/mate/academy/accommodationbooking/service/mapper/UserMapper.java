package mate.academy.accommodationbooking.service.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import mate.academy.accommodationbooking.service.config.MapperConfig;
import mate.academy.accommodationbooking.service.dto.user.UserResponseDto;
import mate.academy.accommodationbooking.service.dto.user.auth.UserRegisterRequestDto;
import mate.academy.accommodationbooking.service.dto.user.crud.UserPatchRequestDto;
import mate.academy.accommodationbooking.service.dto.user.crud.UserUpdateRequestDto;
import mate.academy.accommodationbooking.service.model.User;

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
