package mate.academy.accommodationbookingservice.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.user.UserResponseDto;
import mate.academy.accommodationbookingservice.dto.user.crud.UserPatchRequestDto;
import mate.academy.accommodationbookingservice.dto.user.crud.UserRolesUpdateDto;
import mate.academy.accommodationbookingservice.dto.user.crud.UserUpdateRequestDto;
import mate.academy.accommodationbookingservice.service.user.UserService;

@Tag(
        name = "Users",
        description = "Provide CRUD method related ro user entity"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @Operation(description = "Give's opportunity to "
            + "change role by ID for ADMIN user")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}/role")
    public UserResponseDto updateUserRoles(
            @PathVariable Long id,
            @RequestBody @Valid UserRolesUpdateDto request
    ) {
        return userService.updateRoles(id, request);
    }

    @Operation(description = "Give user opportunity to check about themself")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @GetMapping("/me")
    public UserResponseDto getUserDetailsByAuth(Authentication authentication) {
        return userService.getUserDetails(authentication);
    }

    @Operation(description = "Give user opportunity to update password, email"
            + " and full information about themself. All fields requires")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @PutMapping("/me")
    public UserResponseDto updateUserDetailsById(
            Authentication authentication,
            @RequestBody @Valid UserUpdateRequestDto request
    ) {
        return userService.updateUserDetails(authentication, request);
    }

    @Operation(description = "Give user opportunity to update password, email"
            + " and full information about themself. Any fields can be changed separately")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @PatchMapping("/me")
    public UserResponseDto patchUserDetailsById(
            Authentication authentication,
            @RequestBody @Valid UserPatchRequestDto request
    ) {
        return userService.patchUserDetails(authentication, request);
    }

}
