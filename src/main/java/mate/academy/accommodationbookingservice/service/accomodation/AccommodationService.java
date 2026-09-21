package mate.academy.accommodationbookingservice.service.accomodation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationPatchRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationShortResponseDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationUpdateRequestDto;

public interface AccommodationService {
    AccommodationResponseDto save(AccommodationRequestDto request);

    Page<AccommodationShortResponseDto> findAll(Pageable pageable);

    AccommodationResponseDto findById(Long id);

    AccommodationResponseDto updateById(Long id, AccommodationUpdateRequestDto request);

    AccommodationResponseDto patchById(Long id, AccommodationPatchRequestDto request);

    void delete(Long id);
}
