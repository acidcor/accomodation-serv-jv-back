package mate.academy.accommodationbooking.service.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.JwtException;
import mate.academy.accommodationbooking.service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {
    private static final String SECRET = "01234567890123456789012345678901";
    private static final String EMAIL = "user@example.com";
    private static final String ERROR_MESSAGE = "Expired or invalid JWT token";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L);
    }

    @Test
    @DisplayName("Generate token: result is valid and contains the user's email")
    void generateToken_validEmail_tokenIsValidAndContainsEmail() {
        // given
        String token = jwtUtil.generateToken(EMAIL);

        // when
        boolean valid = jwtUtil.isValidToken(token);
        String username = jwtUtil.getUsername(token);

        // then
        assertTrue(valid);
        assertEquals(EMAIL, username);
    }

    @Test
    @DisplayName("Validate token: malformed token throws JwtException")
    void isValidToken_garbageToken_throwsJwtException() {
        // given
        String garbageToken = "not.a.jwt";

        // when
        JwtException exception = assertThrows(JwtException.class,
                () -> jwtUtil.isValidToken(garbageToken));

        // then
        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("Validate token: token signed with another secret throws JwtException")
    void isValidToken_signedWithOtherSecret_throwsJwtException() {
        // given
        JwtUtil other = new JwtUtil("ABCDEFGHIJKLMNOPQRSTUVWXYZ012345");
        ReflectionTestUtils.setField(other, "expiration", 60_000L);
        String foreignToken = other.generateToken(EMAIL);

        // when
        JwtException exception = assertThrows(JwtException.class,
                () -> jwtUtil.isValidToken(foreignToken));

        // then
        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    @DisplayName("Validate token: expired token throws JwtException")
    void isValidToken_expiredToken_throwsJwtException() {
        // given
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String expiredToken = jwtUtil.generateToken(EMAIL);

        // when
        JwtException exception = assertThrows(JwtException.class,
                () -> jwtUtil.isValidToken(expiredToken));

        // then
        assertEquals(ERROR_MESSAGE, exception.getMessage());
    }
}