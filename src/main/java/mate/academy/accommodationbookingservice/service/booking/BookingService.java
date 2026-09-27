package mate.academy.accommodationbookingservice.service.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import mate.academy.accommodationbookingservice.dto.SearchParamDto;
import mate.academy.accommodationbookingservice.dto.booking.BookingPatchUpdateRequestDto;
import mate.academy.accommodationbookingservice.dto.booking.BookingRequestDto;
import mate.academy.accommodationbookingservice.dto.booking.BookingResponseDto;

public interface BookingService {
    BookingResponseDto save(BookingRequestDto request, Authentication authentication);

    Page<BookingResponseDto> findAll(Authentication authentication, Pageable pageable);

    BookingResponseDto findById(Long id, Authentication authentication);

    BookingResponseDto putById(BookingRequestDto request, Long id, Authentication authentication);

    BookingResponseDto patchById(BookingPatchUpdateRequestDto request, Long id,
                                 Authentication authentication);

    BookingResponseDto cancelBooking(Long id, Authentication authentication);

    Page<BookingResponseDto> findByIdAndStatus(SearchParamDto request, Pageable pageable);
}
