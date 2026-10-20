package mate.academy.accommodationbooking.service.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import mate.academy.accommodationbooking.service.model.RoleEntity;
import mate.academy.accommodationbooking.service.model.User;
import mate.academy.accommodationbooking.service.repository.AccommodationRepository;
import mate.academy.accommodationbooking.service.repository.BookingRepository;
import mate.academy.accommodationbooking.service.service.booking.impl.BookingServiceImpl;
import mate.academy.accommodationbooking.service.service.specification.SpecificationBuilder;
import mate.academy.accommodationbooking.service.service.telegram.TelegramNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    private static final Long USER_ID = 1L;
    private static final Long BOOKING_ID = 10L;
    private static final Long ACCOMMODATION_ID = 100L;
    private static final Long NEW_ACCOMMODATION_ID = 200L;

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TelegramNotificationService notificationService;
    @Mock
    private AccommodationRepository accommodationRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private SpecificationBuilder<Booking> bookingSpecBuilder;
    @Mock
    private Authentication authentication;
    @Mock
    private Accommodation accommodation;
    @Mock
    private Accommodation newAccommodation;
    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    @DisplayName("Save: creates a booking as AWAIT_PAYMENT and sends a notification")
    void save_validRequest_savesBookingWithAwaitPaymentAndNotifies() {
        // given
        User user = buildUser(Role.CUSTOMER);
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(8);
        BookingRequestDto request = buildRequest(checkIn, checkOut, ACCOMMODATION_ID);
        Booking booking = new Booking();
        BookingResponseDto expected = new BookingResponseDto();

        givenAuthenticatedUser(user);
        given(bookingRepository.existsBookingByUserIdAndStatus(USER_ID,
                BookingStatus.AWAIT_PAYMENT)).willReturn(false);
        given(accommodationRepository.findById(ACCOMMODATION_ID))
                .willReturn(Optional.of(accommodation));
        given(bookingRepository.existsBookingInRequestDates(accommodation, checkIn, checkOut))
                .willReturn(false);
        given(bookingMapper.toEntity(request)).willReturn(booking);
        given(bookingRepository.save(booking)).willReturn(booking);
        given(bookingMapper.toDto(booking)).willReturn(expected);

        // when
        BookingResponseDto actual = bookingService.save(request, authentication);

        // then
        assertSame(expected, actual);
        assertSame(user, booking.getUser());
        assertSame(accommodation, booking.getAccommodation());
        assertEquals(BookingStatus.AWAIT_PAYMENT, booking.getStatus());
        then(notificationService).should().sendBookingCreated(expected);
    }

    @Test
    @DisplayName("Save: throws BookingStatusRestriction when a payment is pending")
    void save_userHasPendingPayment_throwsBookingStatusRestriction() {
        // given
        User user = buildUser(Role.CUSTOMER);
        BookingRequestDto request = buildRequest(LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(8), ACCOMMODATION_ID);

        givenAuthenticatedUser(user);
        given(bookingRepository.existsBookingByUserIdAndStatus(USER_ID,
                BookingStatus.AWAIT_PAYMENT)).willReturn(true);

        // when
        BookingStatusRestriction exception = assertThrows(BookingStatusRestriction.class,
                () -> bookingService.save(request, authentication));

        // then
        assertEquals("Can't create a booking while another payment is pending",
                exception.getMessage());
        then(accommodationRepository).shouldHaveNoInteractions();
        then(bookingRepository).should(never()).save(any(Booking.class));
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Save: throws EntityNotFoundException when accommodation is missing")
    void save_accommodationNotFound_throwsEntityNotFoundException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        BookingRequestDto request = buildRequest(LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(8), ACCOMMODATION_ID);

        givenAuthenticatedUser(user);
        given(bookingRepository.existsBookingByUserIdAndStatus(USER_ID,
                BookingStatus.AWAIT_PAYMENT)).willReturn(false);
        given(accommodationRepository.findById(ACCOMMODATION_ID)).willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> bookingService.save(request, authentication));

        // then
        assertEquals("Can't find accommodation with such ID: " + ACCOMMODATION_ID,
                exception.getMessage());
        then(bookingRepository).should(never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Save: throws BookingDateException when check-in is in the past")
    void save_checkInInThePast_throwsBookingDateException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        BookingRequestDto request = buildRequest(LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(3), ACCOMMODATION_ID);

        givenAuthenticatedUser(user);
        given(bookingRepository.existsBookingByUserIdAndStatus(USER_ID,
                BookingStatus.AWAIT_PAYMENT)).willReturn(false);
        given(accommodationRepository.findById(ACCOMMODATION_ID))
                .willReturn(Optional.of(accommodation));

        // when
        BookingDateException exception = assertThrows(BookingDateException.class,
                () -> bookingService.save(request, authentication));

        // then
        assertEquals("Check in date can't be before current date!", exception.getMessage());
        then(bookingRepository).should(never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Save: throws BookingDateException when check-in is not before check-out")
    void save_checkInNotBeforeCheckOut_throwsBookingDateException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        LocalDate sameDay = LocalDate.now().plusDays(5);
        BookingRequestDto request = buildRequest(sameDay, sameDay, ACCOMMODATION_ID);

        givenAuthenticatedUser(user);
        given(bookingRepository.existsBookingByUserIdAndStatus(USER_ID,
                BookingStatus.AWAIT_PAYMENT)).willReturn(false);
        given(accommodationRepository.findById(ACCOMMODATION_ID))
                .willReturn(Optional.of(accommodation));

        // when
        BookingDateException exception = assertThrows(BookingDateException.class,
                () -> bookingService.save(request, authentication));

        // then
        assertEquals("Check in date must be before check out date!", exception.getMessage());
        then(bookingRepository).should(never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Save: throws BookingDateException when dates are already reserved")
    void save_datesAlreadyReserved_throwsBookingDateException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(8);
        BookingRequestDto request = buildRequest(checkIn, checkOut, ACCOMMODATION_ID);

        givenAuthenticatedUser(user);
        given(bookingRepository.existsBookingByUserIdAndStatus(USER_ID,
                BookingStatus.AWAIT_PAYMENT)).willReturn(false);
        given(accommodationRepository.findById(ACCOMMODATION_ID))
                .willReturn(Optional.of(accommodation));
        given(bookingRepository.existsBookingInRequestDates(accommodation, checkIn, checkOut))
                .willReturn(true);

        // when
        BookingDateException exception = assertThrows(BookingDateException.class,
                () -> bookingService.save(request, authentication));

        // then
        assertEquals(String.format("Date already reserved for date between %s and %s",
                checkIn, checkOut), exception.getMessage());
        then(bookingRepository).should(never()).save(any(Booking.class));
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Find all: returns a mapped page of the user's bookings")
    void findAll_userWithBookings_returnsMappedPage() {
        // given
        User user = buildUser(Role.CUSTOMER);
        Pageable pageable = PageRequest.of(0, 10);
        Booking booking = new Booking();
        BookingResponseDto dto = new BookingResponseDto();

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingsByUser(user, pageable))
                .willReturn(new PageImpl<>(List.of(booking), pageable, 1));
        given(bookingMapper.toDto(booking)).willReturn(dto);

        // when
        Page<BookingResponseDto> actual = bookingService.findAll(authentication, pageable);

        // then
        assertEquals(1, actual.getTotalElements());
        assertSame(dto, actual.getContent().get(0));
    }

    @Test
    @DisplayName("Find by ID: admin looks the booking up by ID only")
    void findById_admin_searchesByIdOnly() {
        // given
        User admin = buildUser(Role.ADMIN);
        Booking booking = new Booking();
        BookingResponseDto expected = new BookingResponseDto();

        givenAuthenticatedUser(admin);
        given(bookingRepository.findById(BOOKING_ID)).willReturn(Optional.of(booking));
        given(bookingMapper.toDto(booking)).willReturn(expected);

        // when
        BookingResponseDto actual = bookingService.findById(BOOKING_ID, authentication);

        // then
        assertSame(expected, actual);
        then(bookingRepository).should(never()).findBookingByIdAndUser(anyLong(), any());
    }

    @Test
    @DisplayName("Find by ID: admin gets EntityNotFoundException when nothing is found")
    void findById_adminBookingNotFound_throwsEntityNotFoundException() {
        // given
        User admin = buildUser(Role.ADMIN);

        givenAuthenticatedUser(admin);
        given(bookingRepository.findById(BOOKING_ID)).willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> bookingService.findById(BOOKING_ID, authentication));

        // then
        assertEquals("Can't find booking with such ID: " + BOOKING_ID, exception.getMessage());
    }

    @Test
    @DisplayName("Find by ID: customer looks the booking up by ID and user")
    void findById_customer_searchesByIdAndUser() {
        // given
        User customer = buildUser(Role.CUSTOMER);
        Booking booking = new Booking();
        BookingResponseDto expected = new BookingResponseDto();

        givenAuthenticatedUser(customer);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, customer))
                .willReturn(Optional.of(booking));
        given(bookingMapper.toDto(booking)).willReturn(expected);

        // when
        BookingResponseDto actual = bookingService.findById(BOOKING_ID, authentication);

        // then
        assertSame(expected, actual);
        then(bookingRepository).should(never()).findById(anyLong());
    }

    @Test
    @DisplayName("Find by ID: customer gets EntityNotFoundException when nothing is found")
    void findById_customerBookingNotFound_throwsEntityNotFoundException() {
        // given
        User customer = buildUser(Role.CUSTOMER);

        givenAuthenticatedUser(customer);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, customer))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> bookingService.findById(BOOKING_ID, authentication));

        // then
        assertEquals("Can't find booking with such ID: " + BOOKING_ID, exception.getMessage());
    }

    @Test
    @DisplayName("Update (PUT): updates the booking and its accommodation, then saves")
    void putById_validRequest_updatesBookingAndAccommodation() {
        // given
        User user = buildUser(Role.CUSTOMER);
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(8);
        BookingRequestDto request = buildRequest(checkIn, checkOut, NEW_ACCOMMODATION_ID);
        Booking booking = buildBooking(accommodation, checkIn, checkOut);
        BookingResponseDto expected = new BookingResponseDto();

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(accommodationRepository.findById(NEW_ACCOMMODATION_ID))
                .willReturn(Optional.of(newAccommodation));
        given(bookingRepository.existsBookingInRequestDatesExcludingId(
                newAccommodation, checkIn, checkOut, BOOKING_ID)).willReturn(false);
        given(bookingRepository.save(booking)).willReturn(booking);
        given(bookingMapper.toDto(booking)).willReturn(expected);

        // when
        BookingResponseDto actual = bookingService.putById(request, BOOKING_ID, authentication);

        // then
        assertSame(expected, actual);
        assertSame(newAccommodation, booking.getAccommodation());
        then(bookingMapper).should().updateEntity(request, booking);
        then(bookingRepository).should().save(booking);
    }

    @Test
    @DisplayName("Update (PUT): throws EntityNotFoundException when booking is missing")
    void putById_bookingNotFound_throwsEntityNotFoundException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        BookingRequestDto request = buildRequest(LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(8), ACCOMMODATION_ID);

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> bookingService.putById(request, BOOKING_ID, authentication));

        // then
        assertEquals("Can't find booking with such ID: " + BOOKING_ID, exception.getMessage());
        then(accommodationRepository).shouldHaveNoInteractions();
        then(bookingRepository).should(never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Update (PUT): throws EntityNotFoundException when accommodation is missing")
    void putById_accommodationNotFound_throwsEntityNotFoundException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        BookingRequestDto request = buildRequest(LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(8), NEW_ACCOMMODATION_ID);
        Booking booking = buildBooking(accommodation, request.getCheckIn(),
                request.getCheckOut());

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(accommodationRepository.findById(NEW_ACCOMMODATION_ID))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> bookingService.putById(request, BOOKING_ID, authentication));

        // then
        assertEquals("Can't find accommodation with such ID: " + NEW_ACCOMMODATION_ID,
                exception.getMessage());
        then(bookingRepository).should(never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Update (PUT): throws BookingDateException when dates overlap another booking")
    void putById_datesOverlapAnotherBooking_throwsBookingDateException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(8);
        BookingRequestDto request = buildRequest(checkIn, checkOut, ACCOMMODATION_ID);
        Booking booking = buildBooking(accommodation, checkIn, checkOut);

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(accommodationRepository.findById(ACCOMMODATION_ID))
                .willReturn(Optional.of(accommodation));
        given(bookingRepository.existsBookingInRequestDatesExcludingId(
                accommodation, checkIn, checkOut, BOOKING_ID)).willReturn(true);

        // when
        BookingDateException exception = assertThrows(BookingDateException.class,
                () -> bookingService.putById(request, BOOKING_ID, authentication));

        // then
        assertEquals(String.format("Date already reserved for date between %s and %s",
                checkIn, checkOut), exception.getMessage());
        then(bookingMapper).should(never()).updateEntity(any(), any());
        then(bookingRepository).should(never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Update (PUT): throws BookingDateException when check-in is not before check-out")
    void putById_checkInNotBeforeCheckOut_throwsBookingDateException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        LocalDate sameDay = LocalDate.now().plusDays(5);
        BookingRequestDto request = buildRequest(sameDay, sameDay, ACCOMMODATION_ID);
        Booking booking = buildBooking(accommodation, sameDay, sameDay);

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(accommodationRepository.findById(ACCOMMODATION_ID))
                .willReturn(Optional.of(accommodation));

        // when
        BookingDateException exception = assertThrows(BookingDateException.class,
                () -> bookingService.putById(request, BOOKING_ID, authentication));

        // then
        assertEquals("Check in date must be before check out date!", exception.getMessage());
        then(bookingRepository).should(never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Patch: keeps the current accommodation and saves")
    void patchById_withoutAccommodation_keepsCurrentAccommodationAndSaves() {
        // given
        User user = buildUser(Role.CUSTOMER);
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(8);
        BookingPatchUpdateRequestDto request = new BookingPatchUpdateRequestDto();
        request.setCheckOut(checkOut);
        Booking booking = buildBooking(accommodation, checkIn, checkOut);
        BookingResponseDto expected = new BookingResponseDto();

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(bookingRepository.existsBookingInRequestDatesExcludingId(
                accommodation, checkIn, checkOut, BOOKING_ID)).willReturn(false);
        given(bookingRepository.save(booking)).willReturn(booking);
        given(bookingMapper.toDto(booking)).willReturn(expected);

        // when
        BookingResponseDto actual = bookingService.patchById(request, BOOKING_ID,
                authentication);

        // then
        assertSame(expected, actual);
        assertSame(accommodation, booking.getAccommodation());
        then(bookingMapper).should().patchEntity(request, booking);
        then(accommodationRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Patch: changes the accommodation when a new one is given")
    void patchById_withNewAccommodation_changesAccommodation() {
        // given
        User user = buildUser(Role.CUSTOMER);
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(8);
        BookingPatchUpdateRequestDto request = new BookingPatchUpdateRequestDto();
        request.setAccommodation(NEW_ACCOMMODATION_ID);
        Booking booking = buildBooking(accommodation, checkIn, checkOut);
        BookingResponseDto expected = new BookingResponseDto();

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(accommodationRepository.findById(NEW_ACCOMMODATION_ID))
                .willReturn(Optional.of(newAccommodation));
        given(bookingRepository.existsBookingInRequestDatesExcludingId(
                newAccommodation, checkIn, checkOut, BOOKING_ID)).willReturn(false);
        given(bookingRepository.save(booking)).willReturn(booking);
        given(bookingMapper.toDto(booking)).willReturn(expected);

        // when
        BookingResponseDto actual = bookingService.patchById(request, BOOKING_ID,
                authentication);

        // then
        assertSame(expected, actual);
        assertSame(newAccommodation, booking.getAccommodation());
    }

    @Test
    @DisplayName("Patch: throws EntityNotFoundException when booking is missing")
    void patchById_bookingNotFound_throwsEntityNotFoundException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        BookingPatchUpdateRequestDto request = new BookingPatchUpdateRequestDto();

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> bookingService.patchById(request, BOOKING_ID, authentication));

        // then
        assertEquals("Can't find booking with such ID: " + BOOKING_ID, exception.getMessage());
        then(bookingMapper).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Patch: throws EntityNotFoundException when accommodation is missing")
    void patchById_accommodationNotFound_throwsEntityNotFoundException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        BookingPatchUpdateRequestDto request = new BookingPatchUpdateRequestDto();
        request.setAccommodation(NEW_ACCOMMODATION_ID);
        Booking booking = buildBooking(accommodation, LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(8));

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(accommodationRepository.findById(NEW_ACCOMMODATION_ID))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> bookingService.patchById(request, BOOKING_ID, authentication));

        // then
        assertEquals("Can't find accommodation with such ID: " + NEW_ACCOMMODATION_ID,
                exception.getMessage());
        then(bookingRepository).should(never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Patch: throws BookingDateException when the resulting dates are invalid")
    void patchById_resultingDatesInvalid_throwsBookingDateException() {
        // given: the mapper is mocked, so the booking already holds the "patched" dates
        User user = buildUser(Role.CUSTOMER);
        LocalDate checkIn = LocalDate.now().plusDays(8);
        LocalDate checkOut = LocalDate.now().plusDays(5);
        BookingPatchUpdateRequestDto request = new BookingPatchUpdateRequestDto();
        request.setCheckIn(checkIn);
        Booking booking = buildBooking(accommodation, checkIn, checkOut);

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));

        // when
        BookingDateException exception = assertThrows(BookingDateException.class,
                () -> bookingService.patchById(request, BOOKING_ID, authentication));

        // then
        assertEquals("Check in date must be before check out date!", exception.getMessage());
        then(bookingRepository).should(never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Patch: throws BookingDateException when dates overlap another booking")
    void patchById_datesOverlapAnotherBooking_throwsBookingDateException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(8);
        BookingPatchUpdateRequestDto request = new BookingPatchUpdateRequestDto();
        request.setCheckOut(checkOut);
        Booking booking = buildBooking(accommodation, checkIn, checkOut);

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(bookingRepository.existsBookingInRequestDatesExcludingId(
                accommodation, checkIn, checkOut, BOOKING_ID)).willReturn(true);

        // when
        BookingDateException exception = assertThrows(BookingDateException.class,
                () -> bookingService.patchById(request, BOOKING_ID, authentication));

        // then
        assertEquals(String.format("Date already reserved for date between %s and %s",
                checkIn, checkOut), exception.getMessage());
        then(bookingRepository).should(never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Cancel: sets CANCELED and sends a notification")
    void cancelBooking_activeBooking_setsCanceledAndNotifies() {
        // given
        User user = buildUser(Role.CUSTOMER);
        Booking booking = buildBooking(accommodation, LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(8));
        booking.setStatus(BookingStatus.AWAIT_PAYMENT);
        BookingResponseDto expected = new BookingResponseDto();

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(bookingRepository.save(booking)).willReturn(booking);
        given(bookingMapper.toDto(booking)).willReturn(expected);

        // when
        BookingResponseDto actual = bookingService.cancelBooking(BOOKING_ID, authentication);

        // then
        assertSame(expected, actual);
        assertEquals(BookingStatus.CANCELED, booking.getStatus());
        then(notificationService).should().sendBookingCanceled(expected);
    }

    @Test
    @DisplayName("Cancel: throws BookingCancelingException when already canceled")
    void cancelBooking_alreadyCanceled_throwsBookingCancelingException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        Booking booking = buildBooking(accommodation, LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(8));
        booking.setStatus(BookingStatus.CANCELED);

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));

        // when
        BookingCancelingException exception = assertThrows(BookingCancelingException.class,
                () -> bookingService.cancelBooking(BOOKING_ID, authentication));

        // then
        assertEquals("Booking can't be canceled twice!", exception.getMessage());
        then(bookingRepository).should(never()).save(any(Booking.class));
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Cancel: throws EntityNotFoundException when booking is missing")
    void cancelBooking_bookingNotFound_throwsEntityNotFoundException() {
        // given
        User user = buildUser(Role.CUSTOMER);

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> bookingService.cancelBooking(BOOKING_ID, authentication));

        // then
        assertEquals("Can't find booking with such ID: " + BOOKING_ID, exception.getMessage());
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Find by ID and status: returns a mapped page for the search params")
    void findByIdAndStatus_searchParams_returnsMappedPage() {
        // given
        SearchParamDto request = mock(SearchParamDto.class);
        Specification<Booking> spec = (root, query, cb) -> null;
        Pageable pageable = PageRequest.of(0, 10);
        Booking booking = new Booking();
        BookingResponseDto dto = new BookingResponseDto();

        given(bookingSpecBuilder.build(request)).willReturn(spec);
        given(bookingRepository.findAll(spec, pageable))
                .willReturn(new PageImpl<>(List.of(booking), pageable, 1));
        given(bookingMapper.toDto(booking)).willReturn(dto);

        // when
        Page<BookingResponseDto> actual = bookingService.findByIdAndStatus(request, pageable);

        // then
        assertEquals(1, actual.getTotalElements());
        assertSame(dto, actual.getContent().get(0));
    }

    @Test
    @DisplayName("Expire bookings: sets EXPIRED, saves all and notifies for each")
    void expireBookings_bookingsFound_setsExpiredSavesAndNotifies() {
        // given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Booking first = buildBooking(accommodation, LocalDate.now().minusDays(3),
                LocalDate.now().minusDays(1));
        first.setStatus(BookingStatus.AWAIT_PAYMENT);
        Booking second = buildBooking(accommodation, LocalDate.now().minusDays(5),
                LocalDate.now());
        second.setStatus(BookingStatus.CONFIRMED);
        List<Booking> found = List.of(first, second);
        BookingResponseDto firstDto = new BookingResponseDto();
        BookingResponseDto secondDto = new BookingResponseDto();

        given(bookingRepository.findBookingsByCheckOutLessThanEqualAndStatusNot(
                tomorrow, BookingStatus.CANCELED)).willReturn(found);
        given(bookingRepository.saveAll(found)).willReturn(found);
        given(bookingMapper.toDto(first)).willReturn(firstDto);
        given(bookingMapper.toDto(second)).willReturn(secondDto);

        // when
        bookingService.expireBookings();

        // then
        assertEquals(BookingStatus.EXPIRED, first.getStatus());
        assertEquals(BookingStatus.EXPIRED, second.getStatus());
        then(bookingRepository).should().saveAll(found);
        then(notificationService).should().sendBookingExpired(firstDto);
        then(notificationService).should().sendBookingExpired(secondDto);
    }

    @Test
    @DisplayName("Expire bookings: nothing found means no notification")
    void expireBookings_nothingFound_doesNotNotify() {
        // given
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        given(bookingRepository.findBookingsByCheckOutLessThanEqualAndStatusNot(
                tomorrow, BookingStatus.CANCELED)).willReturn(List.of());
        given(bookingRepository.saveAll(List.of())).willReturn(List.of());

        // when
        bookingService.expireBookings();

        // then
        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Realised bookings: sends notifications without saving anything")
    void realisedBookings_bookingsFound_notifiesWithoutSaving() {
        // given
        LocalDate today = LocalDate.now();
        Booking booking = buildBooking(accommodation, today.minusDays(3), today);
        BookingResponseDto dto = new BookingResponseDto();

        given(bookingRepository.findBookingsByCheckOutLessThanEqualAndStatusNot(
                today, BookingStatus.CONFIRMED)).willReturn(List.of(booking));
        given(bookingMapper.toDto(booking)).willReturn(dto);

        // when
        bookingService.realisedBookings();

        // then
        then(notificationService).should().sendBookingReleased(dto);
        then(bookingRepository).should(never()).saveAll(any());
    }

    private void givenAuthenticatedUser(User user) {
        given(authentication.getPrincipal()).willReturn(user);
    }

    private User buildUser(Role role) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName(role);
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("user@example.com");
        user.setRoles(Set.of(roleEntity));
        return user;
    }

    private BookingRequestDto buildRequest(LocalDate checkIn, LocalDate checkOut,
                                           Long accommodationId) {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setCheckIn(checkIn);
        dto.setCheckOut(checkOut);
        dto.setAccommodation(accommodationId);
        return dto;
    }

    private Booking buildBooking(Accommodation accommodation, LocalDate checkIn,
                                 LocalDate checkOut) {
        Booking booking = new Booking();
        booking.setId(BOOKING_ID);
        booking.setAccommodation(accommodation);
        booking.setStatus(BookingStatus.AWAIT_PAYMENT);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        return booking;
    }
}
