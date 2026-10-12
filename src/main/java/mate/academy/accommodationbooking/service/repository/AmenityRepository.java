package mate.academy.accommodationbooking.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import mate.academy.accommodationbooking.service.model.Amenity;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
}
