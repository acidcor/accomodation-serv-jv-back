package mate.academy.accommodationbookingservice.service.booking.impl;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.booking.BookingPatchUpdateRequestDto;
import mate.academy.accommodationbookingservice.dto.booking.BookingRequestDto;
import mate.academy.accommodationbookingservice.dto.booking.BookingResponseDto;
import mate.academy.accommodationbookingservice.exception.BookingDateException;
import mate.academy.accommodationbookingservice.mapper.BookingMapper;
import mate.academy.accommodationbookingservice.model.Accommodation;
import mate.academy.accommodationbookingservice.model.Booking;
import mate.academy.accommodationbookingservice.model.BookingStatus;
import mate.academy.accommodationbookingservice.model.User;
import mate.academy.accommodationbookingservice.repository.AccommodationRepository;
import mate.academy.accommodationbookingservice.repository.BookingRepository;
import mate.academy.accommodationbookingservice.service.booking.BookingService;

@RequiredArgsConstructor
@Service
@Transactional
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;

    private final AccommodationRepository accommodationRepository;

    private final BookingMapper bookingMapper;

    @Override
    public BookingResponseDto save(BookingRequestDto request, Authentication authentication) {
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

        User user = getUserByAuth(authentication);
        Booking booking = bookingMapper.toEntity(request);

        booking.setUser(user);
        booking.setAccommodation(accommodation);
        booking.setBookingStatus(BookingStatus.PROCESSING);

        return bookingMapper.toDto(bookingRepository.save(booking));
    }

    @Override
    public Page<BookingResponseDto> findAll(Authentication authentication, Pageable pageable) {
        Page<Booking> bookings = bookingRepository.findBookingsByUser(
                getUserByAuth(authentication),
                pageable
        );
        return bookings.map(bookingMapper::toDto);
    }

    @Override
    public BookingResponseDto findById(Long id, Authentication authentication) {
        User user = getUserByAuth(authentication);
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
                        getUserByAuth(authentication)
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find booking with such ID: " + id)
                );
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
                        getUserByAuth(authentication)
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "Can't find booking with such ID: " + id)
                );
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
    public void deleteById(Long id, Authentication authentication) {
        bookingRepository.deleteBookingByIdAndUser(id, getUserByAuth(authentication));
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

    private User getUserByAuth(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user == null) {
            throw new UsernameNotFoundException(
                    "Can't find user with such email: "
                            + authentication.getName()
            );
        }
        return user;
    }
}
