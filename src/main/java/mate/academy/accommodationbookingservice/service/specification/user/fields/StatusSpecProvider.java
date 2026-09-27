package mate.academy.accommodationbookingservice.service.specification.user.fields;

import java.util.Arrays;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import mate.academy.accommodationbookingservice.model.Booking;
import mate.academy.accommodationbookingservice.service.specification.SpecificationProvider;

@Component
public class StatusSpecProvider implements SpecificationProvider<Booking> {
    private static final String FIELD_STATUS = "status";

    @Override
    public String getKey() {
        return FIELD_STATUS;
    }

    @Override
    public Specification<Booking> getSpecification(String[] params) {
        return (root, query, criteriaBuilder)
                -> root.get(FIELD_STATUS).in(Arrays
                .stream(params).toArray());
    }
}
