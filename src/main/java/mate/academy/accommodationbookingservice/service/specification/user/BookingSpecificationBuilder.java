package mate.academy.accommodationbookingservice.service.specification.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import mate.academy.accommodationbookingservice.dto.SearchParamDto;
import mate.academy.accommodationbookingservice.model.Booking;
import mate.academy.accommodationbookingservice.service.specification.SpecificationBuilder;

@Component
public class BookingSpecificationBuilder implements SpecificationBuilder<Booking> {
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_STATUS = "status";
    @Autowired
    private BookingSpecificationProviderManager userSpecProviderManager;

    @Override
    public Specification<Booking> build(SearchParamDto request) {
        Specification<Booking> specification = Specification.unrestricted();
        if (request.getUserId() != null && request.getUserId().length > 0) {
            specification = specification.and(userSpecProviderManager
                    .getSpecificationProvider(FIELD_USER_ID)
                    .getSpecification(request.getUserId()));
        }
        if (request.getStatus() != null && request.getStatus().length > 0) {
            specification = specification.and(userSpecProviderManager
                    .getSpecificationProvider(FIELD_STATUS)
                    .getSpecification(request.getStatus()));
        }
        return specification;
    }
}
