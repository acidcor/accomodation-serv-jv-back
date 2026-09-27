package mate.academy.accommodationbookingservice.service.specification;

import org.springframework.data.jpa.domain.Specification;
import mate.academy.accommodationbookingservice.dto.SearchParamDto;

public interface SpecificationBuilder<T> {
    Specification<T> build(SearchParamDto request);
}
