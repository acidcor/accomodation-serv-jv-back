package mate.academy.accommodationbookingservice.mapper;

import org.mapstruct.Mapper;
import mate.academy.accommodationbookingservice.config.MapperConfig;
import mate.academy.accommodationbookingservice.dto.accomnodation.address.AddressRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.address.AddressResponseDto;
import mate.academy.accommodationbookingservice.model.Address;

@Mapper(config = MapperConfig.class)
public interface AddressMapper {
    Address toEntity(AddressRequestDto request);

    AddressResponseDto toDto(Address entity);
}
