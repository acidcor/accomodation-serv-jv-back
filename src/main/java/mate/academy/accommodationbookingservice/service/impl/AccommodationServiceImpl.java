package mate.academy.accommodationbookingservice.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationPatchRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationRequestDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbookingservice.dto.accomnodation.AccommodationShortResponseDto;
import mate.academy.accommodationbookingservice.mapper.AccommodationMapper;
import mate.academy.accommodationbookingservice.model.Accommodation;
import mate.academy.accommodationbookingservice.repository.AccommodationRepository;
import mate.academy.accommodationbookingservice.service.AccommodationService;

@RequiredArgsConstructor
@Service
@Transactional
public class AccommodationServiceImpl implements AccommodationService {
    private final AccommodationRepository accommodationRepository;

    private final AccommodationMapper accommodationMapper;

    @Override
    public AccommodationResponseDto save(AccommodationRequestDto request) {
        Accommodation accommodation = accommodationMapper.toEntity(request);
        return accommodationMapper.toDto(
                accommodationRepository.save(accommodation)
        );
    }

    @Override
    public Page<AccommodationShortResponseDto> findAll(Pageable pageable) {
        Page<Accommodation> accommodations = accommodationRepository.findAll(pageable);
        return accommodations.map(accommodationMapper::toShortDto);
    }

    @Override
    public AccommodationResponseDto findById(Long id) {
        Accommodation accommodation = accommodationRepository
                .findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException("Can't find accomodation with ID: " + id)
                );
        return accommodationMapper.toDto(accommodation);
    }

    @Override
    public AccommodationResponseDto patchById(Long id, AccommodationPatchRequestDto request) {
        Accommodation accommodation = accommodationRepository
                .findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException("Can't find accomodation with ID: " + id)
                );
        accommodationMapper.patchEntity(request, accommodation);
        return accommodationMapper.toDto(
                accommodationRepository.save(accommodation)
        );
    }

    @Override
    public void delete(Long id) {
        accommodationRepository.deleteById(id);
    }
}
