package mate.academy.accommodationbooking.service.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import mate.academy.accommodationbooking.service.config.MapperConfig;
import mate.academy.accommodationbooking.service.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.AccommodationShortResponseDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationPatchRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationUpdateRequestDto;
import mate.academy.accommodationbooking.service.model.Accommodation;

@Mapper(config = MapperConfig.class, uses = {AddressMapper.class, AmenityMapper.class})
public interface AccommodationMapper {
    AccommodationResponseDto toDto(Accommodation entity);

    AccommodationShortResponseDto toShortDto(Accommodation entity);

    @Mapping(target = "amenities", ignore = true)
    Accommodation toEntity(AccommodationRequestDto requestDto);

    @Mapping(target = "amenities", ignore = true)
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    void patchEntity(AccommodationPatchRequestDto requestDto,
                     @MappingTarget Accommodation entity);

    @Mapping(target = "amenities", ignore = true)
    void updateEntity(AccommodationUpdateRequestDto requestDto,
                      @MappingTarget Accommodation entity);
}
