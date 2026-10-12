package mate.academy.accommodationbooking.service.user;

import static org.springframework.security.test.context.support.TestExecutionEvent.TEST_EXECUTION;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import java.util.Set;
import mate.academy.accommodationbooking.service.dto.user.crud.UserPatchRequestDto;
import mate.academy.accommodationbooking.service.dto.user.crud.UserRolesUpdateDto;
import mate.academy.accommodationbooking.service.dto.user.crud.UserUpdateRequestDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Sql(scripts = "classpath:database/authusers/add-users.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:database/authusers/remove-users.sql", executionPhase = AFTER_TEST_METHOD)
class UserControllerTest {
    private static final String ME_URL = "/users/me";
    private static final String ROLE_URL = "/users/{id}/role";

    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final String NEW_EMAIL = "new.email@example.com";

    private static final Long CUSTOMER_ID = 1000L;
    private static final Long ADMIN_ROLE_ID = 1L;
    private static final Long CUSTOMER_ROLE_ID = 2L;
    private static final Long NOT_EXISTING_ID = 99999L;

    private static MockMvc mockMvc;
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
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void updateUserRoles_adminAndValidRoles_returnsUpdatedUser() throws Exception {
        // given
        UserRolesUpdateDto request = new UserRolesUpdateDto();
        request.setRoles(Set.of(ADMIN_ROLE_ID, CUSTOMER_ROLE_ID));

        // when
        ResultActions result = mockMvc.perform(put(ROLE_URL, CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(CUSTOMER_EMAIL));
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void updateUserRoles_userNotFound_returnsNotFound() throws Exception {
        // given
        UserRolesUpdateDto request = new UserRolesUpdateDto();
        request.setRoles(Set.of(CUSTOMER_ROLE_ID));

        // when
        ResultActions result = mockMvc.perform(put(ROLE_URL, NOT_EXISTING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then (adjust to what your exception handler returns)
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void updateUserRoles_roleNotFound_returnsNotFound() throws Exception {
        // given
        UserRolesUpdateDto request = new UserRolesUpdateDto();
        request.setRoles(Set.of(NOT_EXISTING_ID));

        // when
        ResultActions result = mockMvc.perform(put(ROLE_URL, CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void updateUserRoles_emptyRoles_returnsBadRequest() throws Exception {
        // given
        UserRolesUpdateDto request = new UserRolesUpdateDto();

        // when
        ResultActions result = mockMvc.perform(put(ROLE_URL, CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void updateUserRoles_customer_returnsForbidden() throws Exception {
        // given
        UserRolesUpdateDto request = new UserRolesUpdateDto();
        request.setRoles(Set.of(ADMIN_ROLE_ID));

        // when
        ResultActions result = mockMvc.perform(put(ROLE_URL, CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    void updateUserRoles_noAuthentication_returnsUnauthorized() throws Exception {
        // given
        UserRolesUpdateDto request = new UserRolesUpdateDto();
        request.setRoles(Set.of(CUSTOMER_ROLE_ID));

        // when
        ResultActions result = mockMvc.perform(put(ROLE_URL, CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then (some configs return 403 here instead of 401)
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void getUserDetailsByAuth_customer_returnsOwnDetails() throws Exception {
        // given: customer from the SQL script is authenticated

        // when
        ResultActions result = mockMvc.perform(get(ME_URL));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Customer"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void getUserDetailsByAuth_adminWithoutCustomerRole_returnsForbidden() throws Exception {
        // given: admin has only the ADMIN authority

        // when
        ResultActions result = mockMvc.perform(get(ME_URL));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    void getUserDetailsByAuth_noAuthentication_returnsUnauthorized() throws Exception {
        // given: no authenticated user

        // when
        ResultActions result = mockMvc.perform(get(ME_URL));

        // then
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void updateUserDetailsById_validRequest_returnsUpdatedUser() throws Exception {
        // given
        UserUpdateRequestDto request = buildUpdateRequest(NEW_EMAIL);

        // when
        ResultActions result = mockMvc.perform(put(ME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(NEW_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void updateUserDetailsById_emailTakenByAnotherUser_returnsConflict() throws Exception {
        // given
        UserUpdateRequestDto request = buildUpdateRequest(OTHER_EMAIL);

        // when
        ResultActions result = mockMvc.perform(put(ME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then (adjust to what your handler returns for EmailExistenceException)
        result.andExpect(status().isConflict());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void updateUserDetailsById_passwordsDoNotMatch_returnsBadRequest() throws Exception {
        // given
        UserUpdateRequestDto request = buildUpdateRequest(NEW_EMAIL);
        request.setRepeatPassword("different123");

        // when
        ResultActions result = mockMvc.perform(put(ME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void updateUserDetailsById_blankFirstName_returnsBadRequest() throws Exception {
        // given
        UserUpdateRequestDto request = buildUpdateRequest(NEW_EMAIL);
        request.setFirstName("");

        // when
        ResultActions result = mockMvc.perform(put(ME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void patchUserDetailsById_onlyFirstName_updatesOnlyFirstName() throws Exception {
        // given
        UserPatchRequestDto request = new UserPatchRequestDto();
        request.setFirstName("Patched");

        // when
        ResultActions result = mockMvc.perform(patch(ME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.firstName").value("Patched"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void patchUserDetailsById_emailTakenByAnotherUser_returnsConflict() throws Exception {
        // given
        UserPatchRequestDto request = new UserPatchRequestDto();
        request.setEmail(OTHER_EMAIL);

        // when
        ResultActions result = mockMvc.perform(patch(ME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isConflict());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void patchUserDetailsById_invalidEmailFormat_returnsBadRequest() throws Exception {
        // given
        UserPatchRequestDto request = new UserPatchRequestDto();
        request.setEmail("not-an-email");

        // when
        ResultActions result = mockMvc.perform(patch(ME_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    private UserUpdateRequestDto buildUpdateRequest(String email) {
        UserUpdateRequestDto dto = new UserUpdateRequestDto();
        dto.setEmail(email);
        dto.setFirstName("Updated");
        dto.setLastName("Name");
        dto.setPassword("newPassword123");
        dto.setRepeatPassword("newPassword123");
        return dto;
    }
}
