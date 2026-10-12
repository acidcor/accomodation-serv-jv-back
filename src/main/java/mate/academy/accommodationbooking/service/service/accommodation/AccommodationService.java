package mate.academy.accommodationbooking.service.service.accommodation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import mate.academy.accommodationbooking.service.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.AccommodationShortResponseDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationPatchRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationUpdateRequestDto;

public interface AccommodationService {
    AccommodationResponseDto save(AccommodationRequestDto request);

    Page<AccommodationShortResponseDto> findAll(Pageable pageable);

    AccommodationResponseDto findById(Long id);

    AccommodationResponseDto updateById(Long id, AccommodationUpdateRequestDto request);

    AccommodationResponseDto patchById(Long id, AccommodationPatchRequestDto request);

    void delete(Long id);
}
