package mate.academy.accommodationbooking.service.mapper;

import org.mapstruct.Mapper;
import mate.academy.accommodationbooking.service.config.MapperConfig;
import mate.academy.accommodationbooking.service.dto.accomnodation.address.AddressRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.address.AddressResponseDto;
import mate.academy.accommodationbooking.service.model.Address;

@Mapper(config = MapperConfig.class)
public interface AddressMapper {
    Address toEntity(AddressRequestDto request);

    AddressResponseDto toDto(Address entity);
}
