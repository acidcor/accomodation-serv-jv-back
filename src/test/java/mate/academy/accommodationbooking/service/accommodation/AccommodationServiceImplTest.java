package mate.academy.accommodationbooking.service.accommodation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import mate.academy.accommodationbooking.service.service.accommodation.impl.AccommodationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class AccommodationServiceImplTest {
    private static final Long ID = 7L;

    @Mock
    private AccommodationRepository accommodationRepository;
    @Mock
    private AmenityRepository amenityRepository;
    @Mock
    private AccommodationMapper accommodationMapper;

    @InjectMocks
    private AccommodationServiceImpl accommodationService;

    @Test
    void save_validRequestWithAmenities_savesAccommodationWithResolvedAmenities() {
        // Given
        AccommodationRequestDto request = new AccommodationRequestDto();
        request.setAmenities(List.of(1L, 2L));
        Accommodation mapped = new Accommodation();
        Accommodation saved = accommodation(ID);
        List<Amenity> amenities = List.of(amenity(1L, "Wi-Fi"), amenity(2L, "Parking"));
        AccommodationResponseDto expected = responseDto(ID);

        when(accommodationMapper.toEntity(request)).thenReturn(mapped);
        when(amenityRepository.findAllById(List.of(1L, 2L))).thenReturn(amenities);
        when(accommodationRepository.save(mapped)).thenReturn(saved);
        when(accommodationMapper.toDto(saved)).thenReturn(expected);

        // When
        AccommodationResponseDto actual = accommodationService.save(request);

        // Then
        assertThat(actual).isSameAs(expected);
        assertThat(mapped.getAmenities()).containsExactlyElementsOf(amenities);
        verify(accommodationRepository).save(mapped);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void save_nullOrEmptyAmenities_savesWithEmptyAmenitiesWithoutLookup(List<Long> amenityIds) {
        // Given
        AccommodationRequestDto request = new AccommodationRequestDto();
        request.setAmenities(amenityIds);
        Accommodation mapped = new Accommodation();
        Accommodation saved = accommodation(ID);
        AccommodationResponseDto expected = responseDto(ID);

        when(accommodationMapper.toEntity(request)).thenReturn(mapped);
        when(accommodationRepository.save(mapped)).thenReturn(saved);
        when(accommodationMapper.toDto(saved)).thenReturn(expected);

        // When
        AccommodationResponseDto actual = accommodationService.save(request);

        // Then
        assertThat(actual).isSameAs(expected);
        assertThat(mapped.getAmenities()).isEmpty();
        verifyNoInteractions(amenityRepository);
    }

    @Test
    void save_duplicatedAmenityIds_looksUpEachIdOnlyOnce() {
        // Given
        AccommodationRequestDto request = new AccommodationRequestDto();
        request.setAmenities(List.of(1L, 1L, 2L));
        Accommodation mapped = new Accommodation();
        List<Amenity> amenities = List.of(amenity(1L, "Wi-Fi"), amenity(2L, "Parking"));

        when(accommodationMapper.toEntity(request)).thenReturn(mapped);
        when(amenityRepository.findAllById(List.of(1L, 2L))).thenReturn(amenities);
        when(accommodationRepository.save(mapped)).thenReturn(mapped);
        when(accommodationMapper.toDto(mapped)).thenReturn(responseDto(ID));

        // When
        accommodationService.save(request);

        // Then
        assertThat(mapped.getAmenities()).hasSize(2);
        verify(amenityRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    void save_unknownAmenityIds_throwsNotFoundListingMissingIdsAndDoesNotSave() {
        // Given
        AccommodationRequestDto request = new AccommodationRequestDto();
        request.setAmenities(List.of(1L, 99L, 100L));

        when(accommodationMapper.toEntity(request)).thenReturn(new Accommodation());
        when(amenityRepository.findAllById(List.of(1L, 99L, 100L)))
                .thenReturn(List.of(amenity(1L, "Wi-Fi")));

        // When / Then
        assertThatThrownBy(() -> accommodationService.save(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find amenity(ies) by ID: [99, 100]");
        verify(accommodationRepository, never()).save(any());
    }

    @Test
    void findAll_existingAccommodations_returnsMappedPageKeepingPagingInfo() {
        // Given
        Pageable pageable = PageRequest.of(0, 2);
        Accommodation first = accommodation(1L);
        Accommodation second = accommodation(2L);
        Page<Accommodation> page = new PageImpl<>(List.of(first, second), pageable, 5);
        AccommodationShortResponseDto firstDto = shortDto(1L);
        AccommodationShortResponseDto secondDto = shortDto(2L);

        when(accommodationRepository.findAll(pageable)).thenReturn(page);
        when(accommodationMapper.toShortDto(first)).thenReturn(firstDto);
        when(accommodationMapper.toShortDto(second)).thenReturn(secondDto);

        // When
        Page<AccommodationShortResponseDto> actual = accommodationService.findAll(pageable);

        // Then
        assertThat(actual.getContent()).containsExactly(firstDto, secondDto);
        assertThat(actual.getTotalElements()).isEqualTo(5);
        assertThat(actual.getNumber()).isZero();
    }

    @Test
    void findAll_noAccommodations_returnsEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(accommodationRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        // When
        Page<AccommodationShortResponseDto> actual = accommodationService.findAll(pageable);

        // Then
        assertThat(actual.getContent()).isEmpty();
        verifyNoInteractions(accommodationMapper);
    }

    @Test
    void findById_existingId_returnsMappedDto() {
        // Given
        Accommodation entity = accommodation(ID);
        AccommodationResponseDto expected = responseDto(ID);
        when(accommodationRepository.findById(ID)).thenReturn(Optional.of(entity));
        when(accommodationMapper.toDto(entity)).thenReturn(expected);

        // When
        AccommodationResponseDto actual = accommodationService.findById(ID);

        // Then
        assertThat(actual).isSameAs(expected);
    }

    @Test
    void findById_unknownId_throwsNotFound() {
        // Given
        when(accommodationRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> accommodationService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find accommodation with ID: 99");
        verifyNoInteractions(accommodationMapper);
    }

    @Test
    void updateById_validRequest_updatesEntityReplacesAmenitiesAndSaves() {
        // Given
        AccommodationUpdateRequestDto request = new AccommodationUpdateRequestDto();
        request.setAmenities(List.of(3L));
        Accommodation existing = accommodation(ID);
        existing.setAmenities(new ArrayList<>(List.of(amenity(1L, "Wi-Fi"))));
        Amenity pool = amenity(3L, "Swimming Pool");
        AccommodationResponseDto expected = responseDto(ID);

        when(accommodationRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(amenityRepository.findAllById(List.of(3L))).thenReturn(List.of(pool));
        when(accommodationRepository.save(existing)).thenReturn(existing);
        when(accommodationMapper.toDto(existing)).thenReturn(expected);

        // When
        AccommodationResponseDto actual = accommodationService.updateById(ID, request);

        // Then
        assertThat(actual).isSameAs(expected);
        assertThat(existing.getAmenities()).containsExactly(pool);
        verify(accommodationMapper).updateEntity(request, existing);
        verify(accommodationRepository).save(existing);
    }

    @Test
    void updateById_nullAmenities_clearsExistingAmenities() {
        // Given
        AccommodationUpdateRequestDto request = new AccommodationUpdateRequestDto();
        request.setAmenities(null);
        Accommodation existing = accommodation(ID);
        existing.setAmenities(new ArrayList<>(List.of(amenity(1L, "Wi-Fi"))));

        when(accommodationRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(accommodationRepository.save(existing)).thenReturn(existing);
        when(accommodationMapper.toDto(existing)).thenReturn(responseDto(ID));

        // When
        accommodationService.updateById(ID, request);

        // Then
        assertThat(existing.getAmenities()).isEmpty();
        verifyNoInteractions(amenityRepository);
    }

    @Test
    void updateById_unknownId_throwsNotFoundAndChangesNothing() {
        // Given
        AccommodationUpdateRequestDto request = new AccommodationUpdateRequestDto();
        when(accommodationRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> accommodationService.updateById(99L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find accommodation with ID: 99");
        verifyNoInteractions(accommodationMapper, amenityRepository);
        verify(accommodationRepository, never()).save(any());
    }

    @Test
    void updateById_unknownAmenityId_throwsNotFoundAndDoesNotSave() {
        // Given
        AccommodationUpdateRequestDto request = new AccommodationUpdateRequestDto();
        request.setAmenities(List.of(42L));
        Accommodation existing = accommodation(ID);

        when(accommodationRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(amenityRepository.findAllById(List.of(42L))).thenReturn(List.of());

        // When / Then
        assertThatThrownBy(() -> accommodationService.updateById(ID, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find amenity(ies) by ID: [42]");
        verify(accommodationRepository, never()).save(any());
    }

    @Test
    void patchById_withAmenities_patchesEntityAndReplacesAmenities() {
        // Given
        AccommodationPatchRequestDto request = new AccommodationPatchRequestDto();
        request.setAmenities(List.of(2L));
        Accommodation existing = accommodation(ID);
        Amenity parking = amenity(2L, "Parking");

        when(accommodationRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(amenityRepository.findAllById(List.of(2L))).thenReturn(List.of(parking));
        when(accommodationRepository.save(existing)).thenReturn(existing);
        when(accommodationMapper.toDto(existing)).thenReturn(responseDto(ID));

        // When
        accommodationService.patchById(ID, request);

        // Then
        assertThat(existing.getAmenities()).containsExactly(parking);
        verify(accommodationMapper).patchEntity(request, existing);
    }

    @Test
    void patchById_nullAmenities_keepsExistingAmenities() {
        // Given
        AccommodationPatchRequestDto request = new AccommodationPatchRequestDto();
        request.setAmenities(null);
        Amenity wifi = amenity(1L, "Wi-Fi");
        Accommodation existing = accommodation(ID);
        existing.setAmenities(new ArrayList<>(List.of(wifi)));

        when(accommodationRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(accommodationRepository.save(existing)).thenReturn(existing);
        when(accommodationMapper.toDto(existing)).thenReturn(responseDto(ID));

        // When
        accommodationService.patchById(ID, request);

        // Then
        assertThat(existing.getAmenities()).containsExactly(wifi);
        verifyNoInteractions(amenityRepository);
        verify(accommodationMapper).patchEntity(request, existing);
    }

    @Test
    void patchById_emptyAmenitiesList_clearsExistingAmenities() {
        // Given
        AccommodationPatchRequestDto request = new AccommodationPatchRequestDto();
        request.setAmenities(List.of());
        Accommodation existing = accommodation(ID);
        existing.setAmenities(new ArrayList<>(List.of(amenity(1L, "Wi-Fi"))));

        when(accommodationRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(accommodationRepository.save(existing)).thenReturn(existing);
        when(accommodationMapper.toDto(existing)).thenReturn(responseDto(ID));

        // When
        accommodationService.patchById(ID, request);

        // Then
        assertThat(existing.getAmenities()).isEmpty();
    }

    @Test
    void patchById_unknownId_throwsNotFound() {
        // Given
        AccommodationPatchRequestDto request = new AccommodationPatchRequestDto();
        when(accommodationRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> accommodationService.patchById(99L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find accommodation with ID: 99");
        verifyNoInteractions(accommodationMapper, amenityRepository);
        verify(accommodationRepository, never()).save(any());
    }

    @Test
    void patchById_unknownAmenityId_throwsNotFoundAndDoesNotSave() {
        // Given
        AccommodationPatchRequestDto request = new AccommodationPatchRequestDto();
        request.setAmenities(List.of(1L, 77L));
        Accommodation existing = accommodation(ID);

        when(accommodationRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(amenityRepository.findAllById(List.of(1L, 77L)))
                .thenReturn(List.of(amenity(1L, "Wi-Fi")));

        // When / Then
        assertThatThrownBy(() -> accommodationService.patchById(ID, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find amenity(ies) by ID: [77]");
        verify(accommodationRepository, never()).save(any());
    }

    @Test
    void delete_existingId_deletesById() {
        // Given
        when(accommodationRepository.existsById(ID)).thenReturn(true);

        // When
        accommodationService.delete(ID);

        // Then
        verify(accommodationRepository).deleteById(ID);
    }

    @Test
    void delete_unknownId_throwsNotFoundAndDeletesNothing() {
        // Given
        when(accommodationRepository.existsById(99L)).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> accommodationService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Can't find accommodation with ID: 99");
        verify(accommodationRepository, never()).deleteById(any());
    }

    private static Accommodation accommodation(Long id) {
        Accommodation accommodation = new Accommodation();
        accommodation.setId(id);
        accommodation.setType(Accommodation.Type.APARTMENT);
        accommodation.setSize("2 rooms");
        accommodation.setDailyRate(new BigDecimal("55.50"));
        accommodation.setAvailability(3);
        return accommodation;
    }

    private static Amenity amenity(Long id, String name) {
        Amenity amenity = new Amenity();
        amenity.setId(id);
        amenity.setName(name);
        return amenity;
    }

    private static AccommodationResponseDto responseDto(Long id) {
        return new AccommodationResponseDto(id, Accommodation.Type.APARTMENT, null, "2 rooms",
                List.of(), new BigDecimal("55.50"), 3);
    }

    private static AccommodationShortResponseDto shortDto(Long id) {
        return new AccommodationShortResponseDto(id, Accommodation.Type.APARTMENT, "2 rooms",
                new BigDecimal("55.50"), 3);
    }
}
