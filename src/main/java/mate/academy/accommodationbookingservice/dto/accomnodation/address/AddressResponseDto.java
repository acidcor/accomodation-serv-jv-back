package mate.academy.accommodationbookingservice.dto.accomnodation.address;

public record AddressResponseDto(
        Long id,
        String streetLineOne,
        String streetLineTwo,
        String postalCode,
        String city,
        String region,
        String roomNumber,
        double latitude,
        double longitude
) {
}
