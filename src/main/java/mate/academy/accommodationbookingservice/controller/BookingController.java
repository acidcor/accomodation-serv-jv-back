package mate.academy.accommodationbookingservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.SearchParamDto;
import mate.academy.accommodationbookingservice.dto.booking.BookingPatchUpdateRequestDto;
import mate.academy.accommodationbookingservice.dto.booking.BookingRequestDto;
import mate.academy.accommodationbookingservice.dto.booking.BookingResponseDto;
import mate.academy.accommodationbookingservice.service.booking.BookingService;

@Tag(name = "Booking", description = "Provide CRUD related to Booking entity.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/bookings")
public class BookingController {
    private final BookingService bookingService;

    @Operation(description = "Save new booking.")
    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponseDto saveBooking(@RequestBody @Valid BookingRequestDto request,
                                          Authentication authentication) {
        return bookingService.save(request, authentication);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/")
    public Page<BookingResponseDto> findAllBookingsByIdAndStatus(
            SearchParamDto request,
            Pageable pageable) {
        return bookingService.findByIdAndStatus(request, pageable);
    }

    @Operation(description = "Find all booking created by authenticated user")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @GetMapping("/my")
    public Page<BookingResponseDto> findAllBookingsByAuth(Authentication authentication,
                                                          Pageable pageable) {
        return bookingService.findAll(authentication, pageable);
    }

    @Operation(description = "Show booking details by booking ID created by user")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @GetMapping("/{id}")
    public BookingResponseDto findBookingById(@PathVariable Long id,
                                              Authentication authentication) {
        return bookingService.findById(id, authentication);
    }

    @Operation(description = "Provide update booking details by booking ID created by user")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @PutMapping("/{id}")
    public BookingResponseDto putBookingById(@RequestBody @Valid BookingRequestDto request,
                                             @PathVariable Long id,
                                             Authentication authentication) {
        return bookingService.putById(request, id, authentication);
    }

    @Operation(description = "Provide separated fields update at"
            + " booking details by booking ID created by user")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    @PatchMapping("/{id}")
    public BookingResponseDto patchBookingById(
            @RequestBody @Valid BookingPatchUpdateRequestDto request,
            @PathVariable Long id,
            Authentication authentication
    ) {
        return bookingService.patchById(request, id, authentication);
    }

    @Operation(description = "Cancel user booking")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public BookingResponseDto cancelBookingById(
            @PathVariable Long id, Authentication authentication) {
        return bookingService.cancelBooking(id, authentication);
    }
}
