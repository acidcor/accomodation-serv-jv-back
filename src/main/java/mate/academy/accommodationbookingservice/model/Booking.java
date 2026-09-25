package mate.academy.accommodationbookingservice.model;

import java.time.LocalDate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
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
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    private LocalDate checkIn;
    @NotNull
    private LocalDate checkOut;
    @NotNull
    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(nullable = false)
    private Accommodation accommodation;
    @NotNull
    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(nullable = false)
    private User user;
    @Enumerated(value = EnumType.STRING)
    private BookingStatus bookingStatus;
    @NotNull
    private boolean isDeleted = false;
}
