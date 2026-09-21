package mate.academy.accommodationbookingservice.service.user;

import mate.academy.accommodationbookingservice.dto.user.UserRegisterRequestDto;
import mate.academy.accommodationbookingservice.dto.user.UserResponseDto;
import mate.academy.accommodationbookingservice.exception.RegistrationException;

public interface UserService {
    UserResponseDto register(UserRegisterRequestDto request) throws RegistrationException;
}
