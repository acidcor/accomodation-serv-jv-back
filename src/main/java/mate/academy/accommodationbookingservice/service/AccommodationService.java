package mate.academy.accommodationbookingservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationPatchRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationShortResponseDto;

public interface AccommodationService {
    AccommodationResponseDto save(AccommodationRequestDto request);

    Page<AccommodationShortResponseDto> findAll(Pageable pageable);

    AccommodationResponseDto findById(Long id);

    AccommodationResponseDto patchById(Long id, AccommodationPatchRequestDto request);

    void delete(Long id);
}
