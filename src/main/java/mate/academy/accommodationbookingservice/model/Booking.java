package mate.academy.accommodationbookingservice.model;

import java.time.LocalDate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@SQLDelete(sql = "UPDATE bookings SET is_deleted=true WHERE id = ?")
@SQLRestriction("is_deleted <> true")
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDate checkIn;
    @Column(nullable = false)
    private LocalDate checkOut;
    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(nullable = false)
    private Accommodation accommodation;
    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(nullable = false)
    private User user;
    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private BookingStatus status;
    @Column(nullable = false)
    private boolean isDeleted = false;
}
