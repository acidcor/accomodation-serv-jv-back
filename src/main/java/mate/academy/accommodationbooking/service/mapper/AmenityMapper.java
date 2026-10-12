package mate.academy.accommodationbooking.service.mapper;

import org.mapstruct.Mapper;
import mate.academy.accommodationbooking.service.config.MapperConfig;
import mate.academy.accommodationbooking.service.dto.accomnodation.amenity.AmenityRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.amenity.AmenityResponseDto;
import mate.academy.accommodationbooking.service.model.Amenity;

@Mapper(config = MapperConfig.class)
public interface AmenityMapper {
    Amenity toEntity(AmenityRequestDto request);

    AmenityResponseDto toDto(Amenity entity);
}
