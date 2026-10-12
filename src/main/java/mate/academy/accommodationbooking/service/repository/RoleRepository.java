package mate.academy.accommodationbooking.service.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import mate.academy.accommodationbooking.service.model.Role;
import mate.academy.accommodationbooking.service.model.RoleEntity;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> getByName(Role role);
}
