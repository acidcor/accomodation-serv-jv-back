package mate.academy.accommodationbookingservice.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import mate.academy.accommodationbookingservice.model.Payment;
import mate.academy.accommodationbookingservice.model.PaymentStatus;
import mate.academy.accommodationbookingservice.model.User;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @EntityGraph(attributePaths = {"booking", "booking.accommodation", "booking.user"})
    Page<Payment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"booking", "booking.accommodation", "booking.user"})
    Page<Payment> findPaymentsByBookingUser(User bookingUser, Pageable pageable);

    @EntityGraph(attributePaths = {"booking", "booking.accommodation", "booking.user"})
    Optional<Payment> findPaymentBySessionIdAndBookingUser(String sessionId, User user);

    @EntityGraph(attributePaths = {"booking"})
    Optional<Payment> findPaymentBySessionId(String id);

    @EntityGraph(attributePaths = {"booking", "booking.accommodation", "booking.user"})
    List<Payment> findPaymentsByStatus(PaymentStatus status);

    @EntityGraph(attributePaths = {"booking", "booking.user"})
    boolean existsPaymentByBookingUserAndStatus(User user, PaymentStatus status);
}
