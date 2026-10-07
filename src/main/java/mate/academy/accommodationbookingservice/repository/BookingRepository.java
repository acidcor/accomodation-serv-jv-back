package mate.academy.accommodationbookingservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import mate.academy.accommodationbookingservice.model.Accommodation;
import mate.academy.accommodationbookingservice.model.Booking;
import mate.academy.accommodationbookingservice.model.BookingStatus;
import mate.academy.accommodationbookingservice.model.User;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>,
        JpaSpecificationExecutor<Booking> {
    @Query("""
            SELECT COUNT(b) > 0
            FROM Booking b
            WHERE b.accommodation = :accommodation
              AND b.checkIn < :checkOut
              AND b.checkOut > :checkIn
              AND b.status NOT IN ('CANCELED', 'EXPIRED')
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
              AND b.status NOT IN ('CANCELED', 'EXPIRED')
            """)
    boolean existsBookingInRequestDatesExcludingId(
            Accommodation accommodation,
            LocalDate checkIn,
            LocalDate checkOut,
            Long bookingId
    );

    @EntityGraph(attributePaths = {"user",
            "accommodation",
            "accommodation.amenities",
            "accommodation.location"})
    Page<Booking> findBookingsByUser(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user",
            "accommodation",
            "accommodation.amenities",
            "accommodation.location"})
    Optional<Booking> findBookingByIdAndUser(Long id, User user);

    @EntityGraph(attributePaths = {"user",
            "accommodation",
            "accommodation.amenities",
            "accommodation.location"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    List<Booking> findBookingsByCheckOutLessThanEqualAndStatusNot(
            LocalDate tomorrow,
            BookingStatus bookingStatus
    );

    boolean existsBookingByUser_IdAndStatus(Long userId, BookingStatus status);
}
