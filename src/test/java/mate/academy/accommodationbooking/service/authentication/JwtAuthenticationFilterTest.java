package mate.academy.accommodationbooking.service.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import io.jsonwebtoken.JwtException;
import mate.academy.accommodationbooking.service.security.JwtAuthenticationFilter;
import mate.academy.accommodationbooking.service.security.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    private static final String EMAIL = "user@example.com";
    private static final String TOKEN = "some.jwt.token";

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserDetailsService userDetailsService;
    @InjectMocks
    private JwtAuthenticationFilter filter;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Filter: no Authorization header means no authentication, chain continues")
    void doFilter_noAuthorizationHeader_noAuthenticationAndChainContinues() throws Exception {
        // given: request without Authorization header

        // when
        filter.doFilter(request, response, chain);

        // then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
        then(jwtUtil).shouldHaveNoInteractions();
        then(userDetailsService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Filter: non-Bearer header is ignored, chain continues")
    void doFilter_headerWithoutBearerPrefix_noAuthenticationAndChainContinues()
            throws Exception {
        // given
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic abc123");

        // when
        filter.doFilter(request, response, chain);

        // then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
        then(jwtUtil).shouldHaveNoInteractions();
        then(userDetailsService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Filter: valid Bearer token authenticates the user, chain continues")
    void doFilter_validToken_setsAuthenticationAndChainContinues() throws Exception {
        // given
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        UserDetails userDetails = User.withUsername(EMAIL)
                .password("encoded")
                .authorities("ROLE_USER")
                .build();

        given(jwtUtil.isValidToken(TOKEN)).willReturn(true);
        given(jwtUtil.getUsername(TOKEN)).willReturn(EMAIL);
        given(userDetailsService.loadUserByUsername(EMAIL)).willReturn(userDetails);

        // when
        filter.doFilter(request, response, chain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(EMAIL, ((UserDetails) auth.getPrincipal()).getUsername());
        assertNotNull(chain.getRequest());
    }

    @Test
    @DisplayName("Filter: invalid token means no authentication, chain continues")
    void doFilter_tokenReportedInvalid_noAuthenticationAndChainContinues() throws Exception {
        // given
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        given(jwtUtil.isValidToken(TOKEN)).willReturn(false);

        // when
        filter.doFilter(request, response, chain);

        // then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
        then(userDetailsService).should(never()).loadUserByUsername(EMAIL);
    }

    @Test
    @DisplayName("Filter: JwtException from JwtUtil propagates, chain is not called")
    void doFilter_jwtUtilThrows_exceptionPropagatesAndChainNotCalled() {
        // given
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        given(jwtUtil.isValidToken(TOKEN)).willThrow(new JwtException("Expired or invalid"));

        // when
        JwtException exception = assertThrows(JwtException.class,
                () -> filter.doFilter(request, response, chain));

        // then
        assertEquals("Expired or invalid", exception.getMessage());
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
