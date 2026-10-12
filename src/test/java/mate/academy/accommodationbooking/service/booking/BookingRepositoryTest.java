package mate.academy.accommodationbooking.service.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import mate.academy.accommodationbooking.service.model.Accommodation;
import mate.academy.accommodationbooking.service.model.Booking;
import mate.academy.accommodationbooking.service.model.BookingStatus;
import mate.academy.accommodationbooking.service.model.User;
import mate.academy.accommodationbooking.service.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@Sql(scripts = BookingRepositoryTest.ADD_DATA_SQL, executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = BookingRepositoryTest.REMOVE_DATA_SQL, executionPhase = AFTER_TEST_METHOD)
class BookingRepositoryTest {
    static final String ADD_DATA_SQL = "classpath:database/booking/add-bookings-data.sql";
    static final String REMOVE_DATA_SQL = "classpath:database/booking/remove-bookings-data.sql";

    private static final Long USER_1_ID = 1000L;
    private static final Long USER_2_ID = 1001L;
    private static final Long ACCOMMODATION_1_ID = 1000L;
    private static final Long ACCOMMODATION_2_ID = 1001L;

    private static final Long AWAIT_PAYMENT_BOOKING_ID = 1000L;
    private static final Long CANCELED_BOOKING_ID = 1001L;
    private static final Long CONFIRMED_BOOKING_ID = 1002L;
    private static final Long DELETED_BOOKING_ID = 1004L;

    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void existsBookingInRequestDates_overlappingActiveBooking_returnsTrue() {
        // given
        Accommodation accommodation = accommodation(ACCOMMODATION_1_ID);

        // when
        boolean actual = bookingRepository.existsBookingInRequestDates(
                accommodation, LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 18));

        // then
        assertTrue(actual);
    }

    @Test
    void existsBookingInRequestDates_checkInEqualsExistingCheckOut_returnsFalse() {
        // given
        Accommodation accommodation = accommodation(ACCOMMODATION_1_ID);

        // when
        boolean actual = bookingRepository.existsBookingInRequestDates(
                accommodation, LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 18));

        // then
        assertFalse(actual);
    }

    @Test
    void existsBookingInRequestDates_onlyCanceledBookingOverlaps_returnsFalse() {
        // given
        Accommodation accommodation = accommodation(ACCOMMODATION_1_ID);

        // when
        boolean actual = bookingRepository.existsBookingInRequestDates(
                accommodation, LocalDate.of(2026, 12, 21), LocalDate.of(2026, 12, 23));

        // then
        assertFalse(actual);
    }

    @Test
    void existsBookingInRequestDates_onlyExpiredBookingOverlaps_returnsFalse() {
        // given
        Accommodation accommodation = accommodation(ACCOMMODATION_2_ID);

        // when
        boolean actual = bookingRepository.existsBookingInRequestDates(
                accommodation, LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 14));

        // then
        assertFalse(actual);
    }

    @Test
    void existsBookingInRequestDates_onlySoftDeletedBookingOverlaps_returnsFalse() {
        // given
        Accommodation accommodation = accommodation(ACCOMMODATION_1_ID);

        // when
        boolean actual = bookingRepository.existsBookingInRequestDates(
                accommodation, LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 2));

        // then
        assertFalse(actual);
    }

    @Test
    void existsBookingInRequestDatesExcludingId_onlyOverlapIsExcludedBooking_returnsFalse() {
        // given
        Accommodation accommodation = accommodation(ACCOMMODATION_1_ID);

        // when
        boolean actual = bookingRepository.existsBookingInRequestDatesExcludingId(
                accommodation, LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 15),
                AWAIT_PAYMENT_BOOKING_ID);

        // then
        assertFalse(actual);
    }

    @Test
    void existsBookingInRequestDatesExcludingId_overlapWithAnotherBooking_returnsTrue() {
        // given
        Accommodation accommodation = accommodation(ACCOMMODATION_1_ID);

        // when
        boolean actual = bookingRepository.existsBookingInRequestDatesExcludingId(
                accommodation, LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 15),
                CONFIRMED_BOOKING_ID);

        // then
        assertTrue(actual);
    }

    @Test
    void findBookingsByUser_userWithBookings_returnsOnlyOwnNotDeletedBookings() {
        // given
        User user = user(USER_1_ID);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Booking> actual = bookingRepository.findBookingsByUser(user, pageable);

        // then
        Set<Long> ids = actual.getContent().stream()
                .map(Booking::getId)
                .collect(Collectors.toSet());
        assertEquals(Set.of(AWAIT_PAYMENT_BOOKING_ID, CANCELED_BOOKING_ID), ids);
        assertEquals(2, actual.getTotalElements());
    }

    @Test
    void findBookingsByUser_pageSizeOne_returnsFirstPageOnly() {
        // given
        User user = user(USER_1_ID);
        Pageable pageable = PageRequest.of(0, 1);

        // when
        Page<Booking> actual = bookingRepository.findBookingsByUser(user, pageable);

        // then
        assertEquals(1, actual.getContent().size());
        assertEquals(2, actual.getTotalElements());
        assertEquals(2, actual.getTotalPages());
    }

    @Test
    void findBookingByIdAndUser_bookingBelongsToUser_returnsBooking() {
        // given
        User user = user(USER_1_ID);

        // when
        Optional<Booking> actual = bookingRepository
                .findBookingByIdAndUser(AWAIT_PAYMENT_BOOKING_ID, user);

        // then
        assertTrue(actual.isPresent());
        assertEquals(AWAIT_PAYMENT_BOOKING_ID, actual.get().getId());
    }

    @Test
    void findBookingByIdAndUser_bookingBelongsToAnotherUser_returnsEmpty() {
        // given
        User anotherUser = user(USER_2_ID);

        // when
        Optional<Booking> actual = bookingRepository
                .findBookingByIdAndUser(AWAIT_PAYMENT_BOOKING_ID, anotherUser);

        // then
        assertTrue(actual.isEmpty());
    }

    @Test
    void findBookingByIdAndUser_bookingSoftDeleted_returnsEmpty() {
        // given
        User user = user(USER_1_ID);

        // when
        Optional<Booking> actual = bookingRepository
                .findBookingByIdAndUser(DELETED_BOOKING_ID, user);

        // then
        assertTrue(actual.isEmpty());
    }

    @Test
    void findAll_specificationByStatus_returnsOnlyMatchingBookings() {
        // given
        Specification<Booking> spec = (root, query, cb) ->
                cb.equal(root.get("status"), BookingStatus.CONFIRMED);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Booking> actual = bookingRepository.findAll(spec, pageable);

        // then
        assertEquals(1, actual.getTotalElements());
        assertEquals(CONFIRMED_BOOKING_ID, actual.getContent().get(0).getId());
    }

    @Test
    void findBookingsByCheckOutLessThanEqualAndStatusNot_matchingBookings_returnsThem() {
        // given
        LocalDate date = LocalDate.of(2026, 12, 15);

        // when
        List<Booking> actual = bookingRepository
                .findBookingsByCheckOutLessThanEqualAndStatusNot(date, BookingStatus.EXPIRED);

        // then
        Set<Long> ids = actual.stream().map(Booking::getId).collect(Collectors.toSet());
        assertEquals(Set.of(AWAIT_PAYMENT_BOOKING_ID, CONFIRMED_BOOKING_ID), ids);
    }

    @Test
    void findBookingsByCheckOutLessThanEqualAndStatusNot_dateBeforeAllCheckOuts_returnsEmpty() {
        // given
        LocalDate date = LocalDate.of(2026, 12, 4);

        // when
        List<Booking> actual = bookingRepository
                .findBookingsByCheckOutLessThanEqualAndStatusNot(date, BookingStatus.EXPIRED);

        // then
        assertTrue(actual.isEmpty());
    }

    @Test
    void existsBookingByUserIdAndStatus_userHasBookingWithStatus_returnsTrue() {
        // given: user 2 owns the EXPIRED booking

        // when
        boolean actual = bookingRepository
                .existsBookingByUserIdAndStatus(USER_2_ID, BookingStatus.EXPIRED);

        // then
        assertTrue(actual);
    }

    @Test
    void existsBookingByUserIdAndStatus_userHasNoBookingWithStatus_returnsFalse() {
        // given: user 1 has no EXPIRED booking

        // when
        boolean actual = bookingRepository
                .existsBookingByUserIdAndStatus(USER_1_ID, BookingStatus.EXPIRED);

        // then
        assertFalse(actual);
    }

    private User user(Long id) {
        return entityManager.getReference(User.class, id);
    }

    private Accommodation accommodation(Long id) {
        return entityManager.getReference(Accommodation.class, id);
    }
}