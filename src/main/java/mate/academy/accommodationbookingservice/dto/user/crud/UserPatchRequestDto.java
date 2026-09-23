package mate.academy.accommodationbookingservice.dto.user.crud;

import org.hibernate.validator.constraints.Length;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import mate.academy.accommodationbookingservice.annotation.FieldMatch;

@Getter
@Setter
@FieldMatch(first = "password", second = "repeatPassword")
public class UserPatchRequestDto {
    @Email
    private String email;
    @Length(max = 32)
    private String firstName;
    @Length(max = 32)
    private String lastName;
    @Length(min = 8, max = 32)
    private String password;
    private String repeatPassword;
}
