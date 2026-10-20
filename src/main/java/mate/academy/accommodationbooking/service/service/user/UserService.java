package mate.academy.accommodationbooking.service.service.user;

import org.springframework.security.core.Authentication;
import mate.academy.accommodationbooking.service.dto.user.UserResponseDto;
import mate.academy.accommodationbooking.service.dto.user.auth.UserRegisterRequestDto;
import mate.academy.accommodationbooking.service.dto.user.crud.UserPatchRequestDto;
import mate.academy.accommodationbooking.service.dto.user.crud.UserRolesUpdateDto;
import mate.academy.accommodationbooking.service.dto.user.crud.UserUpdateRequestDto;
import mate.academy.accommodationbooking.service.exception.validation.RegistrationException;

public interface UserService {
    UserResponseDto register(UserRegisterRequestDto request) throws RegistrationException;

    UserResponseDto updateRoles(Long id, UserRolesUpdateDto request);

    UserResponseDto getUserDetails(Authentication authentication);

    UserResponseDto updateUserDetails(Authentication authentication, UserUpdateRequestDto request);

    UserResponseDto patchUserDetails(Authentication authentication, UserPatchRequestDto request);
}
