package mate.academy.accommodationbooking.service.service.booking.impl;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbooking.service.dto.SearchParamDto;
import mate.academy.accommodationbooking.service.dto.booking.BookingPatchUpdateRequestDto;
import mate.academy.accommodationbooking.service.dto.booking.BookingRequestDto;
import mate.academy.accommodationbooking.service.dto.booking.BookingResponseDto;
import mate.academy.accommodationbooking.service.exception.booking.BookingCancelingException;
import mate.academy.accommodationbooking.service.exception.notfound.EntityNotFoundException;
import mate.academy.accommodationbooking.service.exception.validation.BookingDateException;
import mate.academy.accommodationbooking.service.exception.validation.BookingStatusRestriction;
import mate.academy.accommodationbooking.service.mapper.BookingMapper;
import mate.academy.accommodationbooking.service.model.Accommodation;
import mate.academy.accommodationbooking.service.model.Booking;
import mate.academy.accommodationbooking.service.model.BookingStatus;
import mate.academy.accommodationbooking.service.model.Role;
import mate.academy.accommodationbooking.service.model.User;
import mate.academy.accommodationbooking.service.repository.AccommodationRepository;
import mate.academy.accommodationbooking.service.repository.BookingRepository;
import mate.academy.accommodationbooking.service.service.AuthenticatedUserProvider;
import mate.academy.accommodationbooking.service.service.booking.BookingService;
import mate.academy.accommodationbooking.service.service.specification.SpecificationBuilder;
import mate.academy.accommodationbooking.service.service.telegram.TelegramNotificationService;

@RequiredArgsConstructor
@Service
@Transactional
public class BookingServiceImpl implements BookingService, AuthenticatedUserProvider {
    private final BookingRepository bookingRepository;

    private final TelegramNotificationService notificationService;

    private final AccommodationRepository accommodationRepository;

    private final BookingMapper bookingMapper;

    private final SpecificationBuilder<Booking> bookingSpecBuilder;

    @Override
    public BookingResponseDto save(BookingRequestDto request, Authentication authentication) {
        User user = getUserFromAuth(authentication);
        if (bookingRepository.existsBookingByUserIdAndStatus(user.getId()
                , BookingStatus.AWAIT_PAYMENT)) {
            throw new BookingStatusRestriction(
                    "Can't create a booking while another payment is pending"
            );
        }
        Long accommodationId = request.getAccommodation();
        Accommodation accommodation = accommodationRepository
                .findById(accommodationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find accommodation with such ID: " + accommodationId)
                );
        LocalDate checkIn = request.getCheckIn();
        LocalDate checkOut = request.getCheckOut();

        checkDates(checkIn, checkOut);
        checkBooking(checkIn, checkOut, accommodation);

        Booking booking = bookingMapper.toEntity(request);

        booking.setUser(user);
        booking.setAccommodation(accommodation);
        booking.setStatus(BookingStatus.AWAIT_PAYMENT);
        BookingResponseDto dto = bookingMapper.toDto(bookingRepository.save(booking));
        notificationService.sendBookingCreated(dto);
        return dto;
    }

    @Override
    public Page<BookingResponseDto> findAll(Authentication authentication, Pageable pageable) {
        Page<Booking> bookings = bookingRepository.findBookingsByUser(
                getUserFromAuth(authentication),
                pageable
        );
        return bookings.map(bookingMapper::toDto);
    }

    @Override
    public BookingResponseDto findById(Long id, Authentication authentication) {
        User user = getUserFromAuth(authentication);
        if (user
                .getRoles()
                .stream()
                .anyMatch(
                        role -> role.getName() == Role.ADMIN
                )
        ) {
            return bookingMapper.toDto(
                    bookingRepository
                            .findById(id)
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Can't find booking with such ID: " + id)
                            ));
        }
        return bookingMapper.toDto(
                bookingRepository
                        .findBookingByIdAndUser(id, user)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Can't find booking with such ID: " + id)
                        ));
    }

    @Override
    public BookingResponseDto putById(
            BookingRequestDto request,
            Long id,
            Authentication authentication
    ) {
        Long accommodationId = request.getAccommodation();
        Booking booking = bookingRepository
                .findBookingByIdAndUser(
                        id,
                        getUserFromAuth(authentication)
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find booking with such ID: " + id)
                );
        checkStatusModifying(booking);
        Accommodation accommodation = accommodationRepository
                .findById(accommodationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find accommodation with such ID: " + accommodationId)
                );
        LocalDate checkIn = request.getCheckIn();
        LocalDate checkOut = request.getCheckOut();

        checkDates(checkIn, checkOut);
        checkBookingWithExistedBooking(accommodation, checkIn, checkOut,
                booking.getId());
        bookingMapper.updateEntity(request, booking);
        booking.setAccommodation(accommodation);
        return bookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    public BookingResponseDto patchById(
            BookingPatchUpdateRequestDto request,
            Long id,
            Authentication authentication
    ) {
        Booking booking = bookingRepository
                .findBookingByIdAndUser(
                        id,
                        getUserFromAuth(authentication)
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find booking with such ID: " + id)
                );
        checkStatusModifying(booking);
        bookingMapper.patchEntity(request, booking);
        Long accommodationId = request.getAccommodation();
        Accommodation accommodation = booking.getAccommodation();
        LocalDate checkIn = booking.getCheckIn();
        LocalDate checkOut = booking.getCheckOut();
        checkDates(checkIn, checkOut);
        if (accommodationId != null) {
            accommodation = accommodationRepository
                    .findById(accommodationId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Can't find accommodation with such ID: " + accommodationId)
                    );
        }
        checkBookingWithExistedBooking(accommodation, checkIn, checkOut,
                booking.getId());
        booking.setAccommodation(accommodation);
        return bookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    public BookingResponseDto cancelBooking(Long id, Authentication authentication) {
        Booking booking = bookingRepository
                .findBookingByIdAndUser(id, getUserFromAuth(authentication))
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find booking with such ID: " + id)
                );
        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new BookingCancelingException("Booking can't be canceled twice!");
        }
        booking.setStatus(BookingStatus.CANCELED);
        BookingResponseDto dto = bookingMapper.toDto(bookingRepository.save(booking));
        notificationService.sendBookingCanceled(dto);
        return dto;
    }

    @Override
    public Page<BookingResponseDto> findByIdAndStatus(SearchParamDto request, Pageable pageable) {
        Specification<Booking> spec = bookingSpecBuilder.build(request);
        return bookingRepository
                .findAll(spec, pageable)
                .map(bookingMapper::toDto);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void expireBookings() {
        LocalDate tomorrow = LocalDate
                .now()
                .plusDays(1);
        List<Booking> expiredBookings = bookingRepository
                .findBookingsByCheckOutLessThanEqualAndStatusNot(
                        tomorrow,
                        BookingStatus.CANCELED
                );
        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
        }
        List<Booking> bookings = bookingRepository.saveAll(expiredBookings);

        for (Booking booking : bookings) {
            notificationService.sendBookingExpired(bookingMapper.toDto(booking));
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void realisedBookings() {
        LocalDate tomorrow = LocalDate
                .now();
        List<Booking> realised = bookingRepository
                .findBookingsByCheckOutLessThanEqualAndStatusNot(
                        tomorrow,
                        BookingStatus.CONFIRMED
                );
        for (Booking booking : realised) {
            notificationService.sendBookingReleased(bookingMapper.toDto(booking));
        }
    }

    private void checkBooking(LocalDate checkIn, LocalDate checkOut, Accommodation accommodation) {
        if (bookingRepository
                .existsBookingInRequestDates(
                        accommodation
                        , checkIn
                        , checkOut
                )
        ) {
            throw new BookingDateException(String.format(
                    "Date already reserved for date between %s and %s", checkIn, checkOut)
            );
        }
    }

    private void checkStatusModifying(Booking booking) {
        BookingStatus bookingStatus = booking.getStatus();
        if (bookingStatus != BookingStatus.AWAIT_PAYMENT) {
            throw new BookingStatusRestriction("You can't modify booking with status: "
                    + bookingStatus);
        }
    }

    private void checkBookingWithExistedBooking(
            Accommodation accommodation,
            LocalDate checkIn,
            LocalDate checkOut,
            Long id
    ) {
        if (bookingRepository
                .existsBookingInRequestDatesExcludingId(
                        accommodation,
                        checkIn,
                        checkOut,
                        id
                )
        ) {
            throw new BookingDateException(String.format(
                    "Date already reserved for date between %s and %s", checkIn, checkOut)
            );
        }
    }

    private void checkDates(LocalDate checkIn, LocalDate checkOut) {
        isCheckInIsNotBeforeNow(checkIn);
        isCheckInNotAfterCheckOut(checkIn, checkOut);
    }

    private void isCheckInNotAfterCheckOut(LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            throw new BookingDateException("Check in date must be before check out date!");
        }
    }

    private void isCheckInIsNotBeforeNow(LocalDate checkIn) {
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BookingDateException("Check in date can't be before current date!");
        }
    }
}
