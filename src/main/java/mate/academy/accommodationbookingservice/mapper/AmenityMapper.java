package mate.academy.accommodationbookingservice.mapper;

import org.mapstruct.Mapper;
import mate.academy.accommodationbookingservice.config.MapperConfig;
import mate.academy.accommodationbookingservice.dto.accomnodation.amenity.AmenityRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.amenity.AmenityResponseDto;
import mate.academy.accommodationbookingservice.model.Amenity;

@Mapper(config = MapperConfig.class)
public interface AmenityMapper {
    Amenity toEntity(AmenityRequestDto request);

    AmenityResponseDto toDto(Amenity entity);
}
