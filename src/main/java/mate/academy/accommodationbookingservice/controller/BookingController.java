package mate.academy.accommodationbookingservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponseDto saveBooking(@RequestBody @Valid BookingRequestDto request,
                                          Authentication authentication) {
        return bookingService.save(request, authentication);
    }

    /*@GetMapping("/bookings/?user_id={id}&status={status}")
    public BookingResponseDto findByIdAndStatus(
            @PathVariable Long id,
            @PathVariable BookingStatus status,
            Authentication authentication
    )*/

    @Operation(description = "Find all booking created by authenticated user")
    @GetMapping("/my")
    public Page<BookingResponseDto> findAllBookingsByAuth(Authentication authentication,
                                                          Pageable pageable) {
        return bookingService.findAll(authentication, pageable);
    }

    @Operation(description = "Show booking details by booking ID created by user")
    @GetMapping("/{id}")
    public BookingResponseDto findBookingById(@PathVariable Long id,
                                              Authentication authentication) {
        return bookingService.findById(id, authentication);
    }

    @Operation(description = "Provide update booking details by booking ID created by user")
    @PutMapping("/{id}")
    public BookingResponseDto putBookingById(@RequestBody @Valid BookingRequestDto request,
                                             @PathVariable Long id,
                                             Authentication authentication) {
        return bookingService.putById(request, id, authentication);
    }

    @Operation(description = "Provide separated fields update at"
            + " booking details by booking ID created by user")
    @PatchMapping("/{id}")
    public BookingResponseDto patchBookingById(
            @RequestBody @Valid BookingPatchUpdateRequestDto request,
            @PathVariable Long id,
            Authentication authentication
    ) {
        return bookingService.patchById(request, id, authentication);
    }

    @Operation(description = "Delete booking")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id, Authentication authentication) {
        bookingService.deleteById(id, authentication);
    }
}
