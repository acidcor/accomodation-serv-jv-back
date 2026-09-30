package mate.academy.accommodationbookingservice.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import mate.academy.accommodationbookingservice.model.Payment;
import mate.academy.accommodationbookingservice.model.User;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @EntityGraph(attributePaths = {"booking", "booking.accommodation", "booking.user"})
    Page<Payment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"booking", "booking.accommodation", "booking.user"})
    Page<Payment> findPaymentsByBookingUser(User bookingUser, Pageable pageable);

    @EntityGraph(attributePaths = {"booking", "booking.accommodation", "booking.user"})
    Optional<Payment> findPaymentBySessionIdAndBookingUser(String sessionId, User user);

    Optional<Payment> findPaymentBySessionId(String id);
}
