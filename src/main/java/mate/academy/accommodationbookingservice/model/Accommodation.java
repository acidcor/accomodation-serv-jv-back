package mate.academy.accommodationbookingservice.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "accommodations")
public class Accommodation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    @OneToOne(
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(nullable = false)
    private Address location;

    @Column(nullable = false)
    private String size;

    @Column(nullable = false)
    @ManyToMany(fetch = FetchType.LAZY)
    private List<Amenity> amenities = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal dailyRate;
    @Column(nullable = false)
    private Integer availability;

    public enum Type {
        COTTAGE,
        APARTMENT,
        BOAT,
        LOG_CABIN,
        CHALET,
        MOTEL,
        APARTHOTEL,
        CARAVAN,
        CAMPING_TENT,
        CAMPING_YURT,
        CAMPING_TAPEE,
        HOSTEL
    }
}
