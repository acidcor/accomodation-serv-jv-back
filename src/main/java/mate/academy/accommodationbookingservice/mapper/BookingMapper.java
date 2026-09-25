package mate.academy.accommodationbookingservice.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import mate.academy.accommodationbookingservice.config.MapperConfig;
import mate.academy.accommodationbookingservice.dto.booking.BookingPatchUpdateRequestDto;
import mate.academy.accommodationbookingservice.dto.booking.BookingRequestDto;
import mate.academy.accommodationbookingservice.dto.booking.BookingResponseDto;
import mate.academy.accommodationbookingservice.model.Booking;

@Mapper(config = MapperConfig.class)
public interface BookingMapper {
    @Mapping(target = "accommodation", ignore = true)
    Booking toEntity(BookingRequestDto request);

    BookingResponseDto toDto(Booking entity);

    @Mapping(target = "accommodation", ignore = true)
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    void patchEntity(BookingPatchUpdateRequestDto request, @MappingTarget Booking entity);

    @Mapping(target = "accommodation", ignore = true)
    void updateEntity(BookingRequestDto request, @MappingTarget Booking entity);
}
