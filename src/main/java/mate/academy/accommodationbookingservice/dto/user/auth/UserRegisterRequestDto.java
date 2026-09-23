package mate.academy.accommodationbookingservice.dto.user.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import mate.academy.accommodationbookingservice.annotation.FieldMatch;

@Getter
@Setter
@FieldMatch(first = "password", second = "repeatPassword")
public class UserRegisterRequestDto {
    @NotBlank
    @Email
    @Size(
            max = 254,
            message = "Max email length is 254"
    )
    private String email;
    @NotBlank
    @Size(
            min = 1,
            max = 32
    )
    private String firstName;
    @NotBlank
    @Size(
            min = 1,
            max = 32
    )
    private String lastName;
    @NotBlank
    @Size(
            min = 8,
            max = 32,
            message = "Password size must be between 8 and 32"
    )
    private String password;
    @NotBlank
    private String repeatPassword;
}
