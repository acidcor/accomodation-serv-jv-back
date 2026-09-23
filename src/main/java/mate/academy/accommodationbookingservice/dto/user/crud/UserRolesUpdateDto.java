package mate.academy.accommodationbookingservice.dto.user.crud;

import java.util.HashSet;
import java.util.Set;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRolesUpdateDto {
    public static final String EMPTY_SET_MESS = "User should have at least 1 role";

    @NotNull(message = EMPTY_SET_MESS)
    @Size(min = 1, message = EMPTY_SET_MESS)
    private Set<Long> roles = new HashSet<>();
}
