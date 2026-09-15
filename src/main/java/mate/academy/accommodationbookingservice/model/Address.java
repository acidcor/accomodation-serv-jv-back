package mate.academy.accommodationbookingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String streetLineOne;
    @Column(nullable = false)
    private String streetLineTwo;
    @Column(nullable = false)
    private String postalCode;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private String region;
    private String apartment;
    private double latitude;
    private double longitude;
}
