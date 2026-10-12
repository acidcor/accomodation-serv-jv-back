package mate.academy.accommodationbooking.service.authentication;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import mate.academy.accommodationbooking.service.dto.user.auth.UserLoginRequestDto;
import mate.academy.accommodationbooking.service.dto.user.auth.UserRegisterRequestDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {
    private static final String REGISTER_URL = "/auth/register";
    private static final String LOGIN_URL = "/auth/login";

    private static final String ADD_USER_SQL = "classpath:database/authusers/add-test-user.sql";
    private static final String REMOVE_USER_SQL = "classpath:database/authusers/remove-test-user.sql";
    private static final String REMOVE_REGISTERED_SQL =
            "classpath:database/authusers/remove-registered-user.sql";

    private static final String EXISTING_EMAIL = "existing.user@example.com";
    private static final String EXISTING_PASSWORD = "password123";
    private static final String NEW_EMAIL = "new.user@example.com";

    protected static MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll(
            @Autowired WebApplicationContext applicationContext
    ) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @Sql(scripts = REMOVE_REGISTERED_SQL, executionPhase = AFTER_TEST_METHOD)
    void register_validRequest_returnsCreatedUser() throws Exception {
        // given
        UserRegisterRequestDto request = buildRegisterRequest(NEW_EMAIL, "password123",
                "password123");

        // when
        ResultActions result = mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(NEW_EMAIL))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @Sql(scripts = ADD_USER_SQL, executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = REMOVE_USER_SQL, executionPhase = AFTER_TEST_METHOD)
    void register_emailAlreadyExists_returnsError() throws Exception {
        // given
        UserRegisterRequestDto request = buildRegisterRequest(EXISTING_EMAIL, "password123",
                "password123");

        // when
        ResultActions result = mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then (adjust the status to what your exception handler returns)
        result.andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(EXISTING_EMAIL)));
    }

    @Test
    void register_passwordsDoNotMatch_returnsBadRequest() throws Exception {
        // given
        UserRegisterRequestDto request = buildRegisterRequest(NEW_EMAIL, "password123",
                "different123");

        // when
        ResultActions result = mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    void register_invalidEmail_returnsBadRequest() throws Exception {
        // given
        UserRegisterRequestDto request = buildRegisterRequest("not-an-email", "password123",
                "password123");

        // when
        ResultActions result = mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }


    @Test
    @Sql(scripts = ADD_USER_SQL, executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = REMOVE_USER_SQL, executionPhase = AFTER_TEST_METHOD)
    void login_validCredentials_returnsToken() throws Exception {
        // given
        UserLoginRequestDto request = buildLoginRequest(EXISTING_EMAIL, EXISTING_PASSWORD);

        // when
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(not(emptyOrNullString())));
    }

    @Test
    @Sql(scripts = ADD_USER_SQL, executionPhase = BEFORE_TEST_METHOD)
    @Sql(scripts = REMOVE_USER_SQL, executionPhase = AFTER_TEST_METHOD)
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        // given
        UserLoginRequestDto request = buildLoginRequest(EXISTING_EMAIL, "wrongPassword");

        // when
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then (adjust if your exception handler maps BadCredentialsException differently)
        result.andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownUser_returnsUnauthorized() throws Exception {
        // given
        UserLoginRequestDto request = buildLoginRequest("ghost@example.com", "password123");

        // when
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isUnauthorized());
    }

    @Test
    void login_blankEmail_returnsBadRequest() throws Exception {
        // given
        UserLoginRequestDto request = buildLoginRequest("", "password123");

        // when
        ResultActions result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    private UserRegisterRequestDto buildRegisterRequest(String email, String password,
                                                        String repeatPassword) {
        UserRegisterRequestDto dto = new UserRegisterRequestDto();
        dto.setEmail(email);
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setPassword(password);
        dto.setRepeatPassword(repeatPassword);
        return dto;
    }

    private UserLoginRequestDto buildLoginRequest(String email, String password) {
        UserLoginRequestDto dto = new UserLoginRequestDto();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }
}
