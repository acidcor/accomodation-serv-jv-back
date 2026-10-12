package mate.academy.accommodationbooking.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import mate.academy.accommodationbooking.service.dto.user.UserResponseDto;
import mate.academy.accommodationbooking.service.dto.user.auth.UserRegisterRequestDto;
import mate.academy.accommodationbooking.service.dto.user.crud.UserPatchRequestDto;
import mate.academy.accommodationbooking.service.dto.user.crud.UserRolesUpdateDto;
import mate.academy.accommodationbooking.service.dto.user.crud.UserUpdateRequestDto;
import mate.academy.accommodationbooking.service.exception.notfound.EntityNotFoundException;
import mate.academy.accommodationbooking.service.exception.validation.EmailExistenceException;
import mate.academy.accommodationbooking.service.exception.validation.RegistrationException;
import mate.academy.accommodationbooking.service.mapper.UserMapper;
import mate.academy.accommodationbooking.service.model.Role;
import mate.academy.accommodationbooking.service.model.RoleEntity;
import mate.academy.accommodationbooking.service.model.User;
import mate.academy.accommodationbooking.service.repository.RoleRepository;
import mate.academy.accommodationbooking.service.repository.UserRepository;
import mate.academy.accommodationbooking.service.service.user.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    private static final Long USER_ID = 1L;
    private static final String EMAIL = "user@example.com";
    private static final String NEW_EMAIL = "new.user@example.com";
    private static final String RAW_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword";

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private Authentication authentication;
    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_validRequest_returnsDtoWithEncodedPasswordAndCustomerRole() {
        // given
        UserRegisterRequestDto request = buildRegisterRequest();
        User mappedUser = buildUser(null, EMAIL);
        RoleEntity customerRole = buildRole(2L, Role.CUSTOMER);
        UserResponseDto expected = new UserResponseDto(EMAIL, "John", "Doe");

        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(userMapper.toEntity(request)).willReturn(mappedUser);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(roleRepository.getByName(Role.CUSTOMER)).willReturn(Optional.of(customerRole));
        given(userRepository.save(mappedUser)).willReturn(mappedUser);
        given(userMapper.toDto(mappedUser)).willReturn(expected);

        // when
        UserResponseDto actual = userService.register(request);

        // then
        assertEquals(expected, actual);
        assertEquals(ENCODED_PASSWORD, mappedUser.getPassword());
        assertEquals(Set.of(customerRole), mappedUser.getRoles());
        then(userRepository).should().save(mappedUser);
    }

    @Test
    void register_emailAlreadyExists_throwsRegistrationException() {
        // given
        UserRegisterRequestDto request = buildRegisterRequest();
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        // when
        RegistrationException exception = assertThrows(RegistrationException.class,
                () -> userService.register(request));

        // then
        assertEquals("User with this email: " + EMAIL + " already exists",
                exception.getMessage());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    void register_defaultRoleMissing_throwsEntityNotFoundException() {
        // given
        UserRegisterRequestDto request = buildRegisterRequest();
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(userMapper.toEntity(request)).willReturn(buildUser(null, EMAIL));
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(roleRepository.getByName(Role.CUSTOMER)).willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.register(request));

        // then
        assertEquals("Can't find default role while registration process",
                exception.getMessage());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    void updateRoles_validRoles_returnsUpdatedDto() {
        // given
        User user = buildUser(USER_ID, EMAIL);
        RoleEntity admin = buildRole(1L, Role.ADMIN);
        RoleEntity customer = buildRole(2L, Role.CUSTOMER);
        UserRolesUpdateDto request = new UserRolesUpdateDto();
        request.setRoles(Set.of(1L, 2L));
        UserResponseDto expected = new UserResponseDto(EMAIL, "John", "Doe");

        given(userRepository.getUserById(USER_ID)).willReturn(Optional.of(user));
        given(roleRepository.findAllById(Set.of(1L, 2L))).willReturn(List.of(admin, customer));
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toDto(user)).willReturn(expected);

        // when
        UserResponseDto actual = userService.updateRoles(USER_ID, request);

        // then
        assertEquals(expected, actual);
        assertEquals(Set.of(admin, customer), user.getRoles());
        then(userRepository).should().save(user);
    }

    @Test
    void updateRoles_userNotFound_throwsEntityNotFoundException() {
        // given
        UserRolesUpdateDto request = new UserRolesUpdateDto();
        request.setRoles(Set.of(1L));
        given(userRepository.getUserById(USER_ID)).willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.updateRoles(USER_ID, request));

        // then
        assertEquals("Can't find user by ID: " + USER_ID, exception.getMessage());
        then(roleRepository).shouldHaveNoInteractions();
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    void updateRoles_someRolesNotFound_throwsWithMissingIds() {
        // given
        User user = buildUser(USER_ID, EMAIL);
        RoleEntity admin = buildRole(1L, Role.ADMIN);
        UserRolesUpdateDto request = new UserRolesUpdateDto();
        request.setRoles(Set.of(1L, 99L));

        given(userRepository.getUserById(USER_ID)).willReturn(Optional.of(user));
        given(roleRepository.findAllById(Set.of(1L, 99L))).willReturn(List.of(admin));

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.updateRoles(USER_ID, request));

        // then
        assertEquals("Can't find roles by ID: [99]", exception.getMessage());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    void getUserDetails_authenticatedUser_returnsDto() {
        // given
        User user = buildUser(USER_ID, EMAIL);
        UserResponseDto expected = new UserResponseDto(EMAIL, "John", "Doe");
        givenAuthenticatedUser(user);
        given(userMapper.toDto(user)).willReturn(expected);

        // when
        UserResponseDto actual = userService.getUserDetails(authentication);

        // then
        assertEquals(expected, actual);
    }

    @Test
    void updateUserDetails_newFreeEmailAndPassword_updatesAndEncodesPassword() {
        // given
        User user = buildUser(USER_ID, EMAIL);
        UserUpdateRequestDto request = buildUpdateRequest(NEW_EMAIL, RAW_PASSWORD);
        UserResponseDto expected = new UserResponseDto(NEW_EMAIL, "John", "Doe");

        givenAuthenticatedUser(user);
        given(userRepository.existsByEmail(NEW_EMAIL)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toDto(user)).willReturn(expected);

        // when
        UserResponseDto actual = userService.updateUserDetails(authentication, request);

        // then
        assertEquals(expected, actual);
        assertEquals(ENCODED_PASSWORD, user.getPassword());
        then(userMapper).should().updateUser(request, user);
        then(userRepository).should().save(user);
    }

    @Test
    void updateUserDetails_sameEmailDifferentCase_skipsExistenceCheck() {
        // given
        User user = buildUser(USER_ID, EMAIL);
        UserUpdateRequestDto request = buildUpdateRequest(EMAIL.toUpperCase(), RAW_PASSWORD);
        UserResponseDto expected = new UserResponseDto(EMAIL, "John", "Doe");

        givenAuthenticatedUser(user);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toDto(user)).willReturn(expected);

        // when
        UserResponseDto actual = userService.updateUserDetails(authentication, request);

        // then
        assertEquals(expected, actual);
        then(userRepository).should(never()).existsByEmail(anyString());
    }

    @Test
    void updateUserDetails_emailTakenByAnotherUser_throwsEmailExistenceException() {
        // given
        User user = buildUser(USER_ID, EMAIL);
        UserUpdateRequestDto request = buildUpdateRequest(NEW_EMAIL, RAW_PASSWORD);

        givenAuthenticatedUser(user);
        given(userRepository.existsByEmail(NEW_EMAIL)).willReturn(true);

        // when
        EmailExistenceException exception = assertThrows(EmailExistenceException.class,
                () -> userService.updateUserDetails(authentication, request));

        // then
        assertTrue(exception.getMessage().contains(NEW_EMAIL));
        then(userMapper).should(never()).updateUser(any(), any());
        then(userRepository).should(never()).save(any(User.class));
    }

    @Test
    void patchUserDetails_emailAndPasswordNull_savesWithoutChecksOrEncoding() {
        // given
        User user = buildUser(USER_ID, EMAIL);
        user.setPassword("oldEncoded");
        UserPatchRequestDto request = new UserPatchRequestDto();
        request.setFirstName("Jane");
        UserResponseDto expected = new UserResponseDto(EMAIL, "Jane", "Doe");

        givenAuthenticatedUser(user);
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toDto(user)).willReturn(expected);

        // when
        UserResponseDto actual = userService.patchUserDetails(authentication, request);

        // then
        assertEquals(expected, actual);
        assertEquals("oldEncoded", user.getPassword());
        then(userMapper).should().patchUser(request, user);
        then(userRepository).should(never()).existsByEmail(anyString());
        then(passwordEncoder).shouldHaveNoInteractions();
    }

    @Test
    void patchUserDetails_newEmailAndPassword_updatesAndEncodesPassword() {
        // given
        User user = buildUser(USER_ID, EMAIL);
        UserPatchRequestDto request = new UserPatchRequestDto();
        request.setEmail(NEW_EMAIL);
        request.setPassword(RAW_PASSWORD);
        UserResponseDto expected = new UserResponseDto(NEW_EMAIL, "John", "Doe");

        givenAuthenticatedUser(user);
        given(userRepository.existsByEmail(NEW_EMAIL)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);
        given(userRepository.save(user)).willReturn(user);
        given(userMapper.toDto(user)).willReturn(expected);

        // when
        UserResponseDto actual = userService.patchUserDetails(authentication, request);

        // then
        assertEquals(expected, actual);
        assertEquals(ENCODED_PASSWORD, user.getPassword());
        then(userMapper).should().patchUser(request, user);
    }

    @Test
    void patchUserDetails_emailTakenByAnotherUser_throwsEmailExistenceException() {
        // given
        User user = buildUser(USER_ID, EMAIL);
        UserPatchRequestDto request = new UserPatchRequestDto();
        request.setEmail(NEW_EMAIL);

        givenAuthenticatedUser(user);
        given(userRepository.existsByEmail(NEW_EMAIL)).willReturn(true);

        // when
        EmailExistenceException exception = assertThrows(EmailExistenceException.class,
                () -> userService.patchUserDetails(authentication, request));

        // then
        assertTrue(exception.getMessage().contains(NEW_EMAIL));
        then(userMapper).should(never()).patchUser(any(), any());
        then(userRepository).should(never()).save(any(User.class));
    }

    private void givenAuthenticatedUser(User user) {
        given(authentication.getPrincipal()).willReturn(user);
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword("oldEncoded");
        return user;
    }

    private RoleEntity buildRole(Long id, Role name) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setName(name);
        return role;
    }

    private UserRegisterRequestDto buildRegisterRequest() {
        UserRegisterRequestDto dto = new UserRegisterRequestDto();
        dto.setEmail(EMAIL);
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setPassword(RAW_PASSWORD);
        dto.setRepeatPassword(RAW_PASSWORD);
        return dto;
    }

    private UserUpdateRequestDto buildUpdateRequest(String email, String password) {
        UserUpdateRequestDto dto = new UserUpdateRequestDto();
        dto.setEmail(email);
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setPassword(password);
        dto.setRepeatPassword(password);
        return dto;
    }
}
