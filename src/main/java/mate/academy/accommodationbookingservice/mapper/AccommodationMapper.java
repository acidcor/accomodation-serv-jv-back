package mate.academy.accommodationbookingservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import mate.academy.accommodationbookingservice.config.MapperConfig;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationPatchRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationShortResponseDto;
import mate.academy.accommodationbookingservice.model.Accommodation;

@Mapper(config = MapperConfig.class)
public interface AccommodationMapper {
    AccommodationResponseDto toDto(Accommodation entity);

    AccommodationShortResponseDto toShortDto(Accommodation entity);

    Accommodation toEntity(AccommodationRequestDto requestDto);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    void patchEntity(AccommodationPatchRequestDto requestDto,
                     @MappingTarget Accommodation entity);

}
