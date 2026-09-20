package mate.academy.accommodationbookingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import mate.academy.accommodationbookingservice.model.Amenity;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {
}
