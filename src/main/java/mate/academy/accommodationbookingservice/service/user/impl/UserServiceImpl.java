package mate.academy.accommodationbookingservice.service.user.impl;

import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.user.UserRegisterRequestDto;
import mate.academy.accommodationbookingservice.dto.user.UserResponseDto;
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
    private final UserMapper mapper;

    @Override
    public UserResponseDto register(UserRegisterRequestDto request)
            throws RegistrationException {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RegistrationException(
                    String.format("User with this email: %s already exists", request.getEmail())
            );
        }
        User userModel = mapper.toEntity(request);

        userModel.setPassword(passwordEncoder.encode(request.getPassword()));

        RoleEntity role = roleRepository
                .getByName(Role.CUSTOMER)
                .orElseThrow(() -> new EntityNotFoundException("Can't find default role "
                        + "while registration process"));

        userModel.setRoles(Set.of(role));
        User user = userRepository.save(userModel);

        return mapper.toDto(user);
    }
}
