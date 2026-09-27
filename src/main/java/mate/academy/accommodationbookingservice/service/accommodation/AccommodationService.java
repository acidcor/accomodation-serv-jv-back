package mate.academy.accommodationbookingservice.service.accommodation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationShortResponseDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.crud.AccommodationPatchRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.crud.AccommodationRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.crud.AccommodationUpdateRequestDto;

public interface AccommodationService {
    AccommodationResponseDto save(AccommodationRequestDto request);

    Page<AccommodationShortResponseDto> findAll(Pageable pageable);

    AccommodationResponseDto findById(Long id);

    AccommodationResponseDto updateById(Long id, AccommodationUpdateRequestDto request);

    AccommodationResponseDto patchById(Long id, AccommodationPatchRequestDto request);

    void delete(Long id);
}
