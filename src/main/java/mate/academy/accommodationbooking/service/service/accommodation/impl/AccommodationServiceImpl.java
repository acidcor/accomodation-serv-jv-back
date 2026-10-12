package mate.academy.accommodationbooking.service.service.accommodation.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbooking.service.dto.accomnodation.AccommodationResponseDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.AccommodationShortResponseDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationPatchRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationRequestDto;
import mate.academy.accommodationbooking.service.dto.accomnodation.crud.AccommodationUpdateRequestDto;
import mate.academy.accommodationbooking.service.exception.notfound.EntityNotFoundException;
import mate.academy.accommodationbooking.service.mapper.AccommodationMapper;
import mate.academy.accommodationbooking.service.model.Accommodation;
import mate.academy.accommodationbooking.service.model.Amenity;
import mate.academy.accommodationbooking.service.repository.AccommodationRepository;
import mate.academy.accommodationbooking.service.repository.AmenityRepository;
import mate.academy.accommodationbooking.service.service.accommodation.AccommodationService;

@RequiredArgsConstructor
@Service
@Transactional
public class AccommodationServiceImpl implements AccommodationService {
    private final AccommodationRepository accommodationRepository;

    private final AmenityRepository amenityRepository;

    private final AccommodationMapper accommodationMapper;

    @Override
    public AccommodationResponseDto save(AccommodationRequestDto request) {
        Accommodation accommodation = accommodationMapper.toEntity(request);
        List<Amenity> amenities = findAmenities(request.getAmenities());
        accommodation.setAmenities(amenities);
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
                        () -> new EntityNotFoundException("Can't find accommodation with ID: " + id)
                );
        return accommodationMapper.toDto(accommodation);
    }

    @Override
    public AccommodationResponseDto updateById(Long id, AccommodationUpdateRequestDto request) {
        Accommodation accommodation = accommodationRepository
                .findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException("Can't find accommodation with ID: " + id)
                );
        accommodationMapper.updateEntity(request, accommodation);
        List<Amenity> amenities = findAmenities(request.getAmenities());
        accommodation.setAmenities(amenities);
        return accommodationMapper.toDto(
                accommodationRepository.save(accommodation)
        );
    }

    @Override
    public AccommodationResponseDto patchById(Long id, AccommodationPatchRequestDto request) {
        Accommodation accommodation = accommodationRepository
                .findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException("Can't find accommodation with ID: " + id)
                );
        accommodationMapper.patchEntity(request, accommodation);
        if (request.getAmenities() != null) {
            List<Amenity> amenities = findAmenities(request.getAmenities());
            accommodation.setAmenities(amenities);
        }
        return accommodationMapper.toDto(
                accommodationRepository.save(accommodation)
        );
    }

    @Override
    public void delete(Long id) {
        if (!accommodationRepository.existsById(id)) {
            throw new EntityNotFoundException("Can't find accommodation with ID: " + id);
        }
        accommodationRepository.deleteById(id);
    }

    private List<Amenity> findAmenities(List<Long> amenitiesId) {
        if (amenitiesId == null || amenitiesId.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> uniqueIds = amenitiesId
                .stream()
                .distinct()
                .toList();
        List<Amenity> amenities = amenityRepository.findAllById(uniqueIds);
        if (amenities.size() != uniqueIds.size()) {
            List<Long> notExisted = uniqueIds
                    .stream()
                    .filter(id -> amenities
                            .stream()
                            .noneMatch(amenity -> amenity
                                    .getId()
                                    .equals(id)))
                    .toList();

            throw new EntityNotFoundException(
                    "Can't find amenity(ies) by ID: " + notExisted
            );
        }
        return amenities;
    }
}
