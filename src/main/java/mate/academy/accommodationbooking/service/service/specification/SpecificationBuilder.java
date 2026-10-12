package mate.academy.accommodationbooking.service.service.specification;

import org.springframework.data.jpa.domain.Specification;
import mate.academy.accommodationbooking.service.dto.SearchParamDto;

public interface SpecificationBuilder<T> {
    Specification<T> build(SearchParamDto request);
}
