package mate.academy.accommodationbookingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import mate.academy.accommodationbookingservice.model.Amenity;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
}
