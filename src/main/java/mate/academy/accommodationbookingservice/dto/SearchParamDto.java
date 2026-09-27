package mate.academy.accommodationbookingservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchParamDto {
    private String[] userId;
    private String[] status;
}
