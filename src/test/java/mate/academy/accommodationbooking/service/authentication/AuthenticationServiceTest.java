package mate.academy.accommodationbooking.service.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import mate.academy.accommodationbooking.service.dto.user.auth.UserLoginRequestDto;
import mate.academy.accommodationbooking.service.dto.user.auth.UserLoginResponseDto;
import mate.academy.accommodationbooking.service.security.AuthenticationService;
import mate.academy.accommodationbooking.service.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "password123";
    private static final String TOKEN = "generated.jwt.token";

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("Authenticate: valid credentials return a JWT token")
    void authenticate_validCredentials_returnsToken() {
        // given
        UserLoginRequestDto request = new UserLoginRequestDto();
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);

        given(authenticationManager.authenticate(any(Authentication.class)))
                .willReturn(authentication);
        given(authentication.getName()).willReturn(EMAIL);
        given(jwtUtil.generateToken(EMAIL)).willReturn(TOKEN);

        // when
        UserLoginResponseDto response = authenticationService.authenticate(request);

        // then
        assertEquals(TOKEN, response.token());
        then(jwtUtil).should().generateToken(EMAIL);
    }

    @Test
    @DisplayName("Authenticate: bad credentials throw and no token is generated")
    void authenticate_badCredentials_throwsAndDoesNotGenerateToken() {
        // given
        UserLoginRequestDto request = new UserLoginRequestDto();
        request.setEmail(EMAIL);
        request.setPassword("wrong");

        given(authenticationManager.authenticate(any(Authentication.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));

        // when
        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> authenticationService.authenticate(request));

        // then
        assertEquals("Bad credentials", exception.getMessage());
        then(jwtUtil).should(never()).generateToken(anyString());
    }
}
