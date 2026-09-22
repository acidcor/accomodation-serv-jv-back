package mate.academy.accommodationbookingservice.dto.user.crud;

import org.hibernate.validator.constraints.Length;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import mate.academy.accommodationbookingservice.annotation.FieldMatch;

@Getter
@Setter
@FieldMatch(first = "password", second = "repeatPassword")
public class UserUpdateRequestDto {
    @Email
    @NotBlank
    private String email;
    @Length(max = 32)
    @NotBlank
    private String firstName;
    @Length(max = 32)
    @NotBlank
    private String lastName;
    @Length(min = 8, max = 32)
    @NotBlank
    private String password;
    @NotBlank
    private String repeatPassword;
}
