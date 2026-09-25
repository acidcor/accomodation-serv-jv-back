package mate.academy.accommodationbookingservice.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import mate.academy.accommodationbookingservice.model.Accommodation;
import mate.academy.accommodationbookingservice.model.Booking;
import mate.academy.accommodationbookingservice.model.User;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("""
            SELECT COUNT(b) > 0
            FROM Booking b
            WHERE b.accommodation = :accommodation
              AND b.checkIn < :checkOut
              AND b.checkOut > :checkIn
              AND b.bookingStatus NOT IN ('CANCELED', 'EXPIRED')
            """)
    boolean existsBookingInRequestDates(Accommodation accommodation, LocalDate checkIn,
                                        LocalDate checkOut);

    @Query("""
            SELECT COUNT(b) > 0
            FROM Booking b
            WHERE b.accommodation = :accommodation
              AND b.id <> :bookingId
              AND b.checkIn < :checkOut
              AND b.checkOut > :checkIn
              AND b.bookingStatus NOT IN ('CANCELED', 'EXPIRED')
            """)
    boolean existsBookingInRequestDatesExcludingId(
            Accommodation accommodation,
            LocalDate checkIn,
            LocalDate checkOut,
            Long bookingId
    );

    @EntityGraph(attributePaths = {"user", "accommodation"})
    Page<Booking> findBookingsByUser(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "accommodation"})
    Optional<Booking> findBookingByIdAndUser(Long id, User user);

    void deleteBookingByIdAndUser(Long id, User userByAuth);
}
