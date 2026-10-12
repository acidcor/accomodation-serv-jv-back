package mate.academy.accommodationbooking.service.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.context.support.TestExecutionEvent.TEST_EXECUTION;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import mate.academy.accommodationbooking.service.dto.booking.BookingPatchUpdateRequestDto;
import mate.academy.accommodationbooking.service.dto.booking.BookingRequestDto;
import mate.academy.accommodationbooking.service.service.telegram.TelegramNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Sql(scripts = BookingControllerTest.ADD_DATA_SQL, executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = BookingControllerTest.ADD_ROLES_SQL, executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = BookingControllerTest.REMOVE_DATA_SQL, executionPhase = AFTER_TEST_METHOD)
class BookingControllerTest {
    static final String ADD_DATA_SQL = "classpath:database/booking/add-bookings-data.sql";
    static final String ADD_ROLES_SQL = "classpath:database/booking/add-roles.sql";
    static final String REMOVE_DATA_SQL = "classpath:database/booking/remove-bookings-data.sql";

    private static final String BOOKINGS_URL = "/bookings";
    private static final String ALL_BOOKINGS_URL = "/bookings/";
    private static final String MY_BOOKINGS_URL = "/bookings/my";
    private static final String BOOKING_BY_ID_URL = "/bookings/{id}";

    private static final String CUSTOMER_EMAIL = "customer@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";

    private static final Long PENDING_BOOKING_ID = 1000L;
    private static final Long CANCELED_BOOKING_ID = 1001L;
    private static final Long FOREIGN_BOOKING_ID = 1002L;
    private static final Long DELETED_BOOKING_ID = 1004L;
    private static final Long NOT_EXISTING_ID = 99999L;

    private static final Long ACCOMMODATION_1_ID = 1000L;
    private static final Long ACCOMMODATION_2_ID = 1001L;


    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TelegramNotificationService notificationService;

    @Test
    @WithUserDetails(value = OTHER_EMAIL, setupBefore = TEST_EXECUTION)
    void saveBooking_validRequest_returnsCreatedBookingAndNotifies() throws Exception {
        // given: this user has no booking in AWAIT_PAYMENT status
        BookingRequestDto request = buildRequest(date(2099, 6, 1), date(2099, 6, 5),
                ACCOMMODATION_1_ID);

        // when
        ResultActions result = mockMvc.perform(post(BOOKINGS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AWAIT_PAYMENT"))
                .andExpect(jsonPath("$.checkIn").value("2099-06-01"))
                .andExpect(jsonPath("$.checkOut").value("2099-06-05"));
        then(notificationService).should().sendBookingCreated(any());
    }

    @Test
    @WithUserDetails(value = OTHER_EMAIL, setupBefore = TEST_EXECUTION)
    void saveBooking_datesOverlapExistingBooking_returnsBadRequest() throws Exception {
        // given: booking 1000 occupies accommodation 1000 from 2026-12-10 to 2026-12-15
        BookingRequestDto request = buildRequest(date(2026, 12, 12), date(2026, 12, 18),
                ACCOMMODATION_1_ID);

        // when
        ResultActions result = mockMvc.perform(post(BOOKINGS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then (adjust to the status your handler returns for BookingDateException)
        result.andExpect(status().isBadRequest());
        then(notificationService).should(never()).sendBookingCreated(any());
    }

    @Test
    @WithUserDetails(value = OTHER_EMAIL, setupBefore = TEST_EXECUTION)
    void saveBooking_checkInInThePast_returnsBadRequest() throws Exception {
        // given
        BookingRequestDto request = buildRequest(LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(2), ACCOMMODATION_1_ID);

        // when
        ResultActions result = mockMvc.perform(post(BOOKINGS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = OTHER_EMAIL, setupBefore = TEST_EXECUTION)
    void saveBooking_accommodationNotFound_returnsNotFound() throws Exception {
        // given
        BookingRequestDto request = buildRequest(date(2099, 6, 1), date(2099, 6, 5),
                NOT_EXISTING_ID);

        // when
        ResultActions result = mockMvc.perform(post(BOOKINGS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void saveBooking_userHasPendingPayment_returnsConflict() throws Exception {
        // given: customer owns booking 1000 in AWAIT_PAYMENT status
        BookingRequestDto request = buildRequest(date(2099, 6, 1), date(2099, 6, 5),
                ACCOMMODATION_1_ID);

        // when
        ResultActions result = mockMvc.perform(post(BOOKINGS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then (adjust to the status your handler returns for BookingStatusRestriction)
        result.andExpect(status().isConflict());
        then(notificationService).should(never()).sendBookingCreated(any());
    }

    @Test
    @WithUserDetails(value = OTHER_EMAIL, setupBefore = TEST_EXECUTION)
    void saveBooking_missingCheckIn_returnsBadRequest() throws Exception {
        // given
        BookingRequestDto request = buildRequest(null, date(2099, 6, 5), ACCOMMODATION_1_ID);

        // when
        ResultActions result = mockMvc.perform(post(BOOKINGS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void saveBooking_admin_returnsForbidden() throws Exception {
        // given
        BookingRequestDto request = buildRequest(date(2099, 6, 1), date(2099, 6, 5),
                ACCOMMODATION_1_ID);

        // when
        ResultActions result = mockMvc.perform(post(BOOKINGS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    void saveBooking_noAuthentication_returnsUnauthorized() throws Exception {
        // given
        BookingRequestDto request = buildRequest(date(2099, 6, 1), date(2099, 6, 5),
                ACCOMMODATION_1_ID);

        // when
        ResultActions result = mockMvc.perform(post(BOOKINGS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then (some configs return 403 here instead of 401)
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void findAllBookingsByIdAndStatus_admin_returnsAllNotDeletedBookings() throws Exception {
        // given: bookings 1000 to 1003 are visible, 1004 is soft-deleted

        // when
        ResultActions result = mockMvc.perform(get(ALL_BOOKINGS_URL));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void findAllBookingsByIdAndStatus_customer_returnsForbidden() throws Exception {
        // given: customer without the ADMIN authority

        // when
        ResultActions result = mockMvc.perform(get(ALL_BOOKINGS_URL));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void findAllBookingsByAuth_customer_returnsOnlyOwnNotDeletedBookings() throws Exception {
        // given: customer owns 1000 and 1001, booking 1004 is soft-deleted

        // when
        ResultActions result = mockMvc.perform(get(MY_BOOKINGS_URL));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void findAllBookingsByAuth_admin_returnsForbidden() throws Exception {
        // given: admin without the CUSTOMER authority

        // when
        ResultActions result = mockMvc.perform(get(MY_BOOKINGS_URL));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void findBookingById_ownBooking_returnsBooking() throws Exception {
        // given: booking 1000 belongs to the customer and is AWAIT_PAYMENT

        // when
        ResultActions result = mockMvc.perform(get(BOOKING_BY_ID_URL, PENDING_BOOKING_ID));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PENDING_BOOKING_ID))
                .andExpect(jsonPath("$.status").value("AWAIT_PAYMENT"));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void findBookingById_bookingOfAnotherUser_returnsNotFound() throws Exception {
        // given: booking 1002 belongs to another user

        // when
        ResultActions result = mockMvc.perform(get(BOOKING_BY_ID_URL, FOREIGN_BOOKING_ID));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void findBookingById_softDeletedBooking_returnsNotFound() throws Exception {
        // given: booking 1004 belongs to the customer but is soft-deleted

        // when
        ResultActions result = mockMvc.perform(get(BOOKING_BY_ID_URL, DELETED_BOOKING_ID));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void findBookingById_notExistingBooking_returnsNotFound() throws Exception {
        // given: no booking with this id

        // when
        ResultActions result = mockMvc.perform(get(BOOKING_BY_ID_URL, NOT_EXISTING_ID));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void putBookingById_validRequest_returnsUpdatedBooking() throws Exception {
        // given: move booking 1000 to accommodation 1001 on free dates
        BookingRequestDto request = buildRequest(date(2099, 4, 1), date(2099, 4, 5),
                ACCOMMODATION_2_ID);

        // when
        ResultActions result = mockMvc.perform(put(BOOKING_BY_ID_URL, PENDING_BOOKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PENDING_BOOKING_ID))
                .andExpect(jsonPath("$.checkIn").value("2099-04-01"))
                .andExpect(jsonPath("$.checkOut").value("2099-04-05"));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void putBookingById_datesOverlapAnotherBooking_returnsBadRequest() throws Exception {
        // given: booking 1002 (CONFIRMED) occupies accommodation 1000 from 2026-12-01 to 12-05
        BookingRequestDto request = buildRequest(date(2026, 12, 2), date(2026, 12, 4),
                ACCOMMODATION_1_ID);

        // when: customer moves their own booking 1000 onto those dates
        ResultActions result = mockMvc.perform(put(BOOKING_BY_ID_URL, PENDING_BOOKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void putBookingById_checkOutBeforeCheckIn_returnsBadRequest() throws Exception {
        // given
        BookingRequestDto request = buildRequest(date(2099, 4, 5), date(2099, 4, 1),
                ACCOMMODATION_1_ID);

        // when
        ResultActions result = mockMvc.perform(put(BOOKING_BY_ID_URL, PENDING_BOOKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void putBookingById_bookingOfAnotherUser_returnsNotFound() throws Exception {
        // given: booking 1002 belongs to another user
        BookingRequestDto request = buildRequest(date(2099, 4, 1), date(2099, 4, 5),
                ACCOMMODATION_1_ID);

        // when
        ResultActions result = mockMvc.perform(put(BOOKING_BY_ID_URL, FOREIGN_BOOKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void putBookingById_missingAccommodation_returnsBadRequest() throws Exception {
        // given
        BookingRequestDto request = buildRequest(date(2099, 4, 1), date(2099, 4, 5), null);

        // when
        ResultActions result = mockMvc.perform(put(BOOKING_BY_ID_URL, PENDING_BOOKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void patchBookingById_onlyCheckOut_updatesOnlyCheckOut() throws Exception {
        // given: booking 1000 runs from 2026-12-10 to 2026-12-15, no booking in 12-15..12-18
        BookingPatchUpdateRequestDto request = new BookingPatchUpdateRequestDto();
        request.setCheckOut(date(2026, 12, 18));

        // when
        ResultActions result = mockMvc.perform(patch(BOOKING_BY_ID_URL, PENDING_BOOKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.checkIn").value("2026-12-10"))
                .andExpect(jsonPath("$.checkOut").value("2026-12-18"));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void patchBookingById_checkInAfterCheckOut_returnsBadRequest() throws Exception {
        // given: current check-out of booking 1000 is 2026-12-15
        BookingPatchUpdateRequestDto request = new BookingPatchUpdateRequestDto();
        request.setCheckIn(date(2026, 12, 20));

        // when
        ResultActions result = mockMvc.perform(patch(BOOKING_BY_ID_URL, PENDING_BOOKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void patchBookingById_overlappingDates_returnsBadRequest() throws Exception {
        // given: booking 1002 (CONFIRMED) occupies accommodation 1000 from 2026-12-01 to 12-05
        BookingPatchUpdateRequestDto request = new BookingPatchUpdateRequestDto();
        request.setCheckIn(date(2026, 12, 2));
        request.setCheckOut(date(2026, 12, 4));

        // when: customer patches their own booking 1000 (accommodation 1000)
        ResultActions result = mockMvc.perform(patch(BOOKING_BY_ID_URL, PENDING_BOOKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void patchBookingById_accommodationNotFound_returnsNotFound() throws Exception {
        // given
        BookingPatchUpdateRequestDto request = new BookingPatchUpdateRequestDto();
        request.setAccommodation(NOT_EXISTING_ID);

        // when
        ResultActions result = mockMvc.perform(patch(BOOKING_BY_ID_URL, PENDING_BOOKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void patchBookingById_bookingOfAnotherUser_returnsNotFound() throws Exception {
        // given: booking 1002 belongs to another user
        BookingPatchUpdateRequestDto request = new BookingPatchUpdateRequestDto();
        request.setCheckOut(date(2026, 12, 8));

        // when
        ResultActions result = mockMvc.perform(patch(BOOKING_BY_ID_URL, FOREIGN_BOOKING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void cancelBookingById_ownActiveBooking_returnsCanceledBookingAndNotifies()
            throws Exception {
        // given: booking 1000 is AWAIT_PAYMENT

        // when
        ResultActions result = mockMvc.perform(delete(BOOKING_BY_ID_URL, PENDING_BOOKING_ID));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PENDING_BOOKING_ID))
                .andExpect(jsonPath("$.status").value("CANCELED"));
        then(notificationService).should().sendBookingCanceled(any());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void cancelBookingById_alreadyCanceled_returnsConflict() throws Exception {
        // given: booking 1001 is already CANCELED

        // when
        ResultActions result = mockMvc.perform(delete(BOOKING_BY_ID_URL, CANCELED_BOOKING_ID));

        // then (adjust to the status your handler returns for BookingCancelingException)
        result.andExpect(status().isConflict());
        then(notificationService).should(never()).sendBookingCanceled(any());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void cancelBookingById_bookingOfAnotherUser_returnsNotFound() throws Exception {
        // given: booking 1002 belongs to another user

        // when
        ResultActions result = mockMvc.perform(delete(BOOKING_BY_ID_URL, FOREIGN_BOOKING_ID));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void cancelBookingById_admin_returnsForbidden() throws Exception {
        // given: admin without the CUSTOMER authority

        // when
        ResultActions result = mockMvc.perform(delete(BOOKING_BY_ID_URL, PENDING_BOOKING_ID));

        // then
        result.andExpect(status().isForbidden());
    }

    private BookingRequestDto buildRequest(LocalDate checkIn, LocalDate checkOut,
                                           Long accommodationId) {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setCheckIn(checkIn);
        dto.setCheckOut(checkOut);
        dto.setAccommodation(accommodationId);
        return dto;
    }

    private LocalDate date(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    private String toJson(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
