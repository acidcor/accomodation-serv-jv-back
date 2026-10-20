package mate.academy.accommodationbooking.service.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbooking.service.dto.user.UserResponseDto;
import mate.academy.accommodationbooking.service.dto.user.auth.UserLoginRequestDto;
import mate.academy.accommodationbooking.service.dto.user.auth.UserLoginResponseDto;
import mate.academy.accommodationbooking.service.dto.user.auth.UserRegisterRequestDto;
import mate.academy.accommodationbooking.service.exception.validation.RegistrationException;
import mate.academy.accommodationbooking.service.security.AuthenticationService;
import mate.academy.accommodationbooking.service.service.user.UserService;

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
