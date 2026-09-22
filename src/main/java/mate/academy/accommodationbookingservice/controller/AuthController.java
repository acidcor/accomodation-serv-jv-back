package mate.academy.accommodationbookingservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.user.UserResponseDto;
import mate.academy.accommodationbookingservice.dto.user.auth.UserLoginRequestDto;
import mate.academy.accommodationbookingservice.dto.user.auth.UserLoginResponseDto;
import mate.academy.accommodationbookingservice.dto.user.auth.UserRegisterRequestDto;
import mate.academy.accommodationbookingservice.exception.RegistrationException;
import mate.academy.accommodationbookingservice.security.AuthenticationService;
import mate.academy.accommodationbookingservice.service.user.UserService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(
        name = "Authentication",
        description = "Provide authentication operations"
)
public class AuthController {
    private final UserService userService;
    private final AuthenticationService authenticationService;

    @Operation(description = "Provide user registration then authentication")
    @PostMapping("/register")
    public UserResponseDto register(@RequestBody @Valid UserRegisterRequestDto request)
            throws RegistrationException {
        return userService.register(request);
    }

    @Operation(description = "Provide user authentication user")
    @PostMapping("/login")
    public UserLoginResponseDto login(@RequestBody @Valid UserLoginRequestDto request) {
        return authenticationService.authenticate(request);
    }
}
