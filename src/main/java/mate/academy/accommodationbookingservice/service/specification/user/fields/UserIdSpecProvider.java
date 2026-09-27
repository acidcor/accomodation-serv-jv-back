package mate.academy.accommodationbookingservice.service.specification.user.fields;

import java.util.Arrays;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import mate.academy.accommodationbookingservice.model.Booking;
import mate.academy.accommodationbookingservice.service.specification.SpecificationProvider;

@Component
public class UserIdSpecProvider implements SpecificationProvider<Booking> {
    private static final String KEY_NAME = "userId";
    private static final String FILED_NAME = "user";
    private static final String SUB_FIELD_NAME = "id";

    @Override
    public String getKey() {
        return KEY_NAME;
    }

    @Override
    public Specification<Booking> getSpecification(String[] params) {
        return (root, query, criteriaBuilder)
                -> root.get(FILED_NAME).get(SUB_FIELD_NAME).in(Arrays.stream(params).toArray());
    }
}
