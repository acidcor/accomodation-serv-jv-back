package mate.academy.accommodationbookingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mate.academy.accommodationbookingservice.model.Accommodation;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {
}
