package mate.academy.accommodationbookingservice.service.user;

import org.springframework.security.core.Authentication;
import mate.academy.accommodationbookingservice.dto.user.UserResponseDto;
import mate.academy.accommodationbookingservice.dto.user.auth.UserRegisterRequestDto;
import mate.academy.accommodationbookingservice.dto.user.crud.UserPatchRequestDto;
import mate.academy.accommodationbookingservice.dto.user.crud.UserRolesUpdateDto;
import mate.academy.accommodationbookingservice.dto.user.crud.UserUpdateRequestDto;
import mate.academy.accommodationbookingservice.exception.RegistrationException;

public interface UserService {
    UserResponseDto register(UserRegisterRequestDto request) throws RegistrationException;

    UserResponseDto updateRoles(Long id, UserRolesUpdateDto request);

    UserResponseDto getUserDetails(Authentication authentication);

    UserResponseDto updateUserDetails(Authentication authentication, UserUpdateRequestDto request);

    UserResponseDto patchUserDetails(Authentication authentication, UserPatchRequestDto request);
}
