package mate.academy.accommodationbookingservice.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import mate.academy.accommodationbookingservice.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    @Query(
            "FROM User u "
                    + "LEFT JOIN FETCH u.roles"
                    + " WHERE u.email = :email"
    )
    Optional<User> getUserByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> getUserById(Long id);
}
