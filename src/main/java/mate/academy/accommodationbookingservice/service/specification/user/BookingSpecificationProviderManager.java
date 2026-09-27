package mate.academy.accommodationbookingservice.service.specification.user;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import mate.academy.accommodationbookingservice.exception.SpecificationProviderNotFoundException;
import mate.academy.accommodationbookingservice.model.Booking;
import mate.academy.accommodationbookingservice.service.specification.SpecificationProvider;
import mate.academy.accommodationbookingservice.service.specification.SpecificationProviderManager;

@Component
public class BookingSpecificationProviderManager implements SpecificationProviderManager<Booking> {
    @Autowired
    private List<SpecificationProvider<Booking>> userSpecProvider;

    @Override
    public SpecificationProvider<Booking> getSpecificationProvider(String key) {
        return userSpecProvider
                .stream()
                .filter(p -> p
                        .getKey()
                        .equals(key))
                .findFirst()
                .orElseThrow(
                        () -> new SpecificationProviderNotFoundException("Can't find current "
                                + "specification provider for key: " + key)
                );
    }
}
