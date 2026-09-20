package mate.academy.accommodationbookingservice.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import mate.academy.accommodationbookingservice.model.Accommodation;

@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {
    @EntityGraph(attributePaths = {"location", "amenities"})
    Optional<Accommodation> findById(Long id);
}
