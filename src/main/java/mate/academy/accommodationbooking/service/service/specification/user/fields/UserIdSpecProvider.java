package mate.academy.accommodationbooking.service.service.specification.user.fields;

import java.util.Arrays;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import mate.academy.accommodationbooking.service.model.Booking;
import mate.academy.accommodationbooking.service.service.specification.SpecificationProvider;

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
