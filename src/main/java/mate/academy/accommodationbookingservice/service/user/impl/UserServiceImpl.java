package mate.academy.accommodationbookingservice.service.user.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.user.UserResponseDto;
import mate.academy.accommodationbookingservice.dto.user.auth.UserRegisterRequestDto;
import mate.academy.accommodationbookingservice.dto.user.crud.UserPatchRequestDto;
import mate.academy.accommodationbookingservice.dto.user.crud.UserRolesUpdateDto;
import mate.academy.accommodationbookingservice.dto.user.crud.UserUpdateRequestDto;
import mate.academy.accommodationbookingservice.exception.EmailExistenceException;
import mate.academy.accommodationbookingservice.exception.RegistrationException;
import mate.academy.accommodationbookingservice.mapper.UserMapper;
import mate.academy.accommodationbookingservice.model.Role;
import mate.academy.accommodationbookingservice.model.RoleEntity;
import mate.academy.accommodationbookingservice.model.User;
import mate.academy.accommodationbookingservice.repository.RoleRepository;
import mate.academy.accommodationbookingservice.repository.UserRepository;
import mate.academy.accommodationbookingservice.service.user.UserService;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final UserMapper userMapper;

    @Override
    public UserResponseDto register(UserRegisterRequestDto request)
            throws RegistrationException {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RegistrationException(
                    String.format("User with this email: %s already exists", request.getEmail())
            );
        }
        User userModel = userMapper.toEntity(request);

        userModel.setPassword(passwordEncoder.encode(request.getPassword()));

        RoleEntity role = roleRepository
                .getByName(Role.CUSTOMER)
                .orElseThrow(() -> new EntityNotFoundException("Can't find default role "
                        + "while registration process"));

        userModel.setRoles(Set.of(role));
        User user = userRepository.save(userModel);

        return userMapper.toDto(user);
    }

    @Override
    public UserResponseDto updateRoles(Long id, UserRolesUpdateDto request) {
        User user = userRepository
                .getUserById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException("Can't find user by ID: " + id)
                );
        Set<Long> rolesIds = request.getRoles();
        List<RoleEntity> roles = roleRepository.findAllById(rolesIds);
        if (rolesIds.size() != roles.size()) {
            Set<Long> notFound = checkRoles(rolesIds, roles);
            throw new EntityNotFoundException("Can't find roles by ID: " + notFound);
        }
        user.setRoles(new HashSet<>(roles));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserResponseDto getUserDetails(Authentication authentication) {
        return userMapper.toDto(getUserByAuth(authentication));
    }

    @Override
    public UserResponseDto updateUserDetails(
            Authentication authentication,
            UserUpdateRequestDto request) {
        User user = getUserByAuth(authentication);
        String requestEmail = request.getEmail();
        String currentEmail = user.getEmail();
        checkEmail(requestEmail, currentEmail);
        userMapper.updateUser(request, user);
        return getResponseDto(
                user,
                request.getPassword()
        );
    }

    @Override
    public UserResponseDto patchUserDetails(
            Authentication authentication,
            UserPatchRequestDto request) {
        User user = getUserByAuth(authentication);
        String requestEmail = request.getEmail();
        if (requestEmail != null) {
            String currentEmail = user.getEmail();
            checkEmail(requestEmail, currentEmail);
        }
        userMapper.patchUser(request, user);
        return getResponseDto(
                user,
                request.getPassword()
        );
    }

    private Set<Long> checkRoles(Set<Long> rolesIds, List<RoleEntity> roles) {
        Set<Long> foundIds = roles
                .stream()
                .map(RoleEntity::getId)
                .collect(Collectors.toSet());

        return rolesIds
                .stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toSet());
    }

    private User getUserByAuth(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user == null) {
            throw new UsernameNotFoundException(
                    "Can't find user with such email: "
                            + authentication.getName()
            );
        }
        return user;
    }

    private UserResponseDto getResponseDto(User user, String rawPass) {
        if (rawPass != null) {
            user.setPassword(getEncodedPass(rawPass));
        }
        return userMapper.toDto(userRepository.save(user));
    }

    private void checkEmail(String requestEmail, String currentEmail) {
        if (!requestEmail.equalsIgnoreCase(currentEmail)
                && userRepository.existsByEmail(requestEmail)) {
            throw new EmailExistenceException(
                    "User with such email already exists: "
                    + requestEmail);
        }
    }

    private String getEncodedPass(String rawPass) {
        return passwordEncoder.encode(rawPass);
    }
}
