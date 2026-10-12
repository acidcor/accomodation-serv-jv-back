package mate.academy.accommodationbooking.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.context.support.TestExecutionEvent.TEST_EXECUTION;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import mate.academy.accommodationbooking.service.dto.payment.PaymentRequestDto;
import mate.academy.accommodationbooking.service.service.stripe.StripeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Sql(scripts = {PaymentControllerTest.REMOVE_DATA_SQL, PaymentControllerTest.ADD_DATA_SQL},
        executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = PaymentControllerTest.REMOVE_DATA_SQL, executionPhase = AFTER_TEST_METHOD)
class PaymentControllerTest {
    static final String ADD_DATA_SQL =
            "classpath:database/payment/add-payment-controller-data.sql";
    static final String REMOVE_DATA_SQL =
            "classpath:database/delete-all.sql";

    private static final String PAYMENTS_URL = "/payments";
    private static final String RENEW_URL = "/payments/{id}/";
    private static final String SYNC_URL = "/payments/{id}/sync";
    private static final String SYNC_ALL_URL = "/payments/all/sync";
    private static final String SUCCESS_URL = "/payments/success";
    private static final String CANCEL_URL = "/payments/cancel";

    private static final String CUSTOMER_EMAIL = "payment.customer@example.com";
    private static final String ADMIN_EMAIL = "payment.admin@example.com";

    private static final Long BOOKING_WITHOUT_PAYMENT_ID = 4000L;
    private static final Long CONFIRMED_BOOKING_ID = 4002L;
    private static final Long FOREIGN_BOOKING_ID = 4004L;

    private static final Long AWAITING_PAYMENT_ID = 4001L;
    private static final Long PAID_PAYMENT_ID = 4002L;
    private static final Long EXPIRED_PAYMENT_ID = 4003L;
    private static final Long FOREIGN_PAYMENT_ID = 4004L;
    private static final Long NOT_EXISTING_ID = 99999L;

    private static final String NEW_SESSION_ID = "sess_new";
    private static final String NEW_SESSION_URL = "https://checkout.stripe.test/new";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private StripeService stripeService;

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void findAll_customer_returnsOnlyOwnPayments() throws Exception {
        // given: customer owns payments 4001, 4002 and 4003

        // when
        ResultActions result = mockMvc.perform(get(PAYMENTS_URL));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void findAll_admin_returnsAllPayments() throws Exception {
        // given: four payments in the SQL script

        // when
        ResultActions result = mockMvc.perform(get(PAYMENTS_URL));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void findAll_noAuthentication_returnsUnauthorized() throws Exception {
        // given: no authenticated user

        // when
        ResultActions result = mockMvc.perform(get(PAYMENTS_URL));

        // then (some configs return 403 here instead of 401)
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void initPayment_awaitPaymentBooking_returnsSessionUrlAndSavesPayment() throws Exception {
        // given: 5 nights * 150.00 = 750
        PaymentRequestDto request = buildRequest(BOOKING_WITHOUT_PAYMENT_ID);
        given(stripeService.getBookingCheckOutSession(
                argThat(amount -> amount.compareTo(new BigDecimal("750")) == 0)))
                .willReturn(buildSession(NEW_SESSION_ID, NEW_SESSION_URL));

        // when
        ResultActions result = mockMvc.perform(post(PAYMENTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionUrl").value(NEW_SESSION_URL));
        assertEquals(NEW_SESSION_ID, jdbcTemplate.queryForObject(
                "SELECT session_id FROM payments WHERE booking_id = ?", String.class,
                BOOKING_WITHOUT_PAYMENT_ID));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void initPayment_bookingOfAnotherUser_returnsNotFound() throws Exception {
        // given
        PaymentRequestDto request = buildRequest(FOREIGN_BOOKING_ID);

        // when
        ResultActions result = mockMvc.perform(post(PAYMENTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isNotFound());
        then(stripeService).shouldHaveNoInteractions();
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void initPayment_notExistingBooking_returnsNotFound() throws Exception {
        // given
        PaymentRequestDto request = buildRequest(NOT_EXISTING_ID);

        // when
        ResultActions result = mockMvc.perform(post(PAYMENTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void initPayment_bookingNotAwaitingPayment_returnsBadRequest() throws Exception {
        // given: booking 4002 is CONFIRMED
        PaymentRequestDto request = buildRequest(CONFIRMED_BOOKING_ID);

        // when
        ResultActions result = mockMvc.perform(post(PAYMENTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then (adjust to the status your handler returns for PaymentInitializationException)
        result.andExpect(status().isBadRequest());
        then(stripeService).shouldHaveNoInteractions();
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void initPayment_stripeReturnsNoSession_returnsBadRequestAndSavesNothing()
            throws Exception {
        // given: the mocked StripeService returns null by default
        PaymentRequestDto request = buildRequest(BOOKING_WITHOUT_PAYMENT_ID);

        // when
        ResultActions result = mockMvc.perform(post(PAYMENTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isBadRequest());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class,
                BOOKING_WITHOUT_PAYMENT_ID));
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void initPayment_admin_returnsForbidden() throws Exception {
        // given: admin without the CUSTOMER authority
        PaymentRequestDto request = buildRequest(BOOKING_WITHOUT_PAYMENT_ID);

        // when
        ResultActions result = mockMvc.perform(post(PAYMENTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    void initPayment_noAuthentication_returnsUnauthorized() throws Exception {
        // given
        PaymentRequestDto request = buildRequest(BOOKING_WITHOUT_PAYMENT_ID);

        // when
        ResultActions result = mockMvc.perform(post(PAYMENTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        // then
        result.andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void renewPayment_expiredPayment_returnsNewSessionAndAwaitPayment() throws Exception {
        // given
        given(stripeService.getBookingCheckOutSession(any(BigDecimal.class)))
                .willReturn(buildSession(NEW_SESSION_ID, NEW_SESSION_URL));

        // when
        ResultActions result = mockMvc.perform(post(RENEW_URL, EXPIRED_PAYMENT_ID));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionUrl").value(NEW_SESSION_URL));
        assertEquals("AWAIT_PAYMENT", paymentStatus(EXPIRED_PAYMENT_ID));
        assertEquals(NEW_SESSION_ID, jdbcTemplate.queryForObject(
                "SELECT session_id FROM payments WHERE id = ?", String.class,
                EXPIRED_PAYMENT_ID));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void renewPayment_awaitingPayment_returnsNewSession() throws Exception {
        // given
        given(stripeService.getBookingCheckOutSession(any(BigDecimal.class)))
                .willReturn(buildSession(NEW_SESSION_ID, NEW_SESSION_URL));

        // when
        ResultActions result = mockMvc.perform(post(RENEW_URL, AWAITING_PAYMENT_ID));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionUrl").value(NEW_SESSION_URL));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void renewPayment_paidPayment_returnsBadRequestAndKeepsPayment() throws Exception {
        // given: payment 4002 is PAID

        // when
        ResultActions result = mockMvc.perform(post(RENEW_URL, PAID_PAYMENT_ID));

        // then (adjust to the status your handler returns for PaymentInitializationException)
        result.andExpect(status().isBadRequest());
        assertEquals("PAID", paymentStatus(PAID_PAYMENT_ID));
        then(stripeService).shouldHaveNoInteractions();
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void renewPayment_paymentOfAnotherUser_returnsNotFound() throws Exception {
        // given: payment 4004 belongs to another user

        // when
        ResultActions result = mockMvc.perform(post(RENEW_URL, FOREIGN_PAYMENT_ID));

        // then
        result.andExpect(status().isNotFound());
        then(stripeService).shouldHaveNoInteractions();
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void renewPayment_notExistingPayment_returnsNotFound() throws Exception {
        // given: no payment with this id

        // when
        ResultActions result = mockMvc.perform(post(RENEW_URL, NOT_EXISTING_ID));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void renewPayment_admin_returnsForbidden() throws Exception {
        // given: admin without the CUSTOMER authority

        // when
        ResultActions result = mockMvc.perform(post(RENEW_URL, EXPIRED_PAYMENT_ID));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void syncPayment_sessionPaid_marksPaymentPaidAndConfirmsBooking() throws Exception {
        // given: payment 4001 (sess_c1) is AWAIT_PAYMENT in the DB
        given(stripeService.retrieveSession("sess_c1"))
                .willReturn(buildSessionWithStatus("complete", "paid"));

        // when
        ResultActions result = mockMvc.perform(get(SYNC_URL, AWAITING_PAYMENT_ID));

        // then
        result.andExpect(status().isOk());
        assertEquals("PAID", paymentStatus(AWAITING_PAYMENT_ID));
        assertEquals("CONFIRMED", bookingStatus(AWAITING_PAYMENT_ID));
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void syncPayment_sessionExpired_marksPaymentExpired() throws Exception {
        // given
        given(stripeService.retrieveSession("sess_c1"))
                .willReturn(buildSessionWithStatus("expired", null));

        // when
        ResultActions result = mockMvc.perform(get(SYNC_URL, AWAITING_PAYMENT_ID));

        // then
        result.andExpect(status().isOk());
        assertEquals("EXPIRED", paymentStatus(AWAITING_PAYMENT_ID));
        assertEquals("AWAIT_PAYMENT", bookingStatus(AWAITING_PAYMENT_ID));
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void syncPayment_notExistingPayment_returnsNotFound() throws Exception {
        // given: no payment with this id

        // when
        ResultActions result = mockMvc.perform(get(SYNC_URL, NOT_EXISTING_ID));

        // then
        result.andExpect(status().isNotFound());
        then(stripeService).shouldHaveNoInteractions();
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void syncPayment_customer_returnsForbidden() throws Exception {
        // given: customer without the ADMIN authority

        // when
        ResultActions result = mockMvc.perform(get(SYNC_URL, AWAITING_PAYMENT_ID));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void syncAllPayments_admin_syncsOnlyAwaitingPayments() throws Exception {
        // given: AWAIT_PAYMENT payments are 4001 (sess_c1) and 4004 (sess_o1)
        given(stripeService.retrieveSession("sess_c1"))
                .willReturn(buildSessionWithStatus("expired", null));
        given(stripeService.retrieveSession("sess_o1"))
                .willReturn(buildSessionWithStatus("complete", "paid"));

        // when
        ResultActions result = mockMvc.perform(get(SYNC_ALL_URL));

        // then
        result.andExpect(status().isOk());
        assertEquals("EXPIRED", paymentStatus(AWAITING_PAYMENT_ID));
        assertEquals("PAID", paymentStatus(FOREIGN_PAYMENT_ID));
        assertEquals("PAID", paymentStatus(PAID_PAYMENT_ID));
        assertEquals("EXPIRED", paymentStatus(EXPIRED_PAYMENT_ID));
        then(stripeService).should(never()).retrieveSession("sess_c2");
        then(stripeService).should(never()).retrieveSession("sess_c3");
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void syncAllPayments_customer_returnsForbidden() throws Exception {
        // given: customer without the ADMIN authority

        // when
        ResultActions result = mockMvc.perform(get(SYNC_ALL_URL));

        // then
        result.andExpect(status().isForbidden());
        then(stripeService).shouldHaveNoInteractions();
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void successPayment_ownSession_returnsPayment() throws Exception {
        // given: session sess_c2 belongs to the customer's payment 4002

        // when
        ResultActions result = mockMvc.perform(get(SUCCESS_URL).param("session_id", "sess_c2"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAID_PAYMENT_ID))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void successPayment_sessionOfAnotherUser_returnsNotFound() throws Exception {
        // given: session sess_o1 belongs to another user

        // when
        ResultActions result = mockMvc.perform(get(SUCCESS_URL).param("session_id", "sess_o1"));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void successPayment_missingSessionId_returnsBadRequest() throws Exception {
        // given: no session_id parameter

        // when
        ResultActions result = mockMvc.perform(get(SUCCESS_URL));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails(value = ADMIN_EMAIL, setupBefore = TEST_EXECUTION)
    void successPayment_admin_returnsForbidden() throws Exception {
        // given: admin without the CUSTOMER authority

        // when
        ResultActions result = mockMvc.perform(get(SUCCESS_URL).param("session_id", "sess_c2"));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void cancelPayment_ownSession_returnsPayment() throws Exception {
        // given: session sess_c1 belongs to the customer's payment 4001

        // when
        ResultActions result = mockMvc.perform(get(CANCEL_URL).param("session_id", "sess_c1"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(AWAITING_PAYMENT_ID))
                .andExpect(jsonPath("$.status").value("AWAIT_PAYMENT"));
    }

    @Test
    @WithUserDetails(value = CUSTOMER_EMAIL, setupBefore = TEST_EXECUTION)
    void cancelPayment_sessionOfAnotherUser_returnsNotFound() throws Exception {
        // given: session sess_o1 belongs to another user

        // when
        ResultActions result = mockMvc.perform(get(CANCEL_URL).param("session_id", "sess_o1"));

        // then
        result.andExpect(status().isNotFound());
    }

    private PaymentRequestDto buildRequest(Long bookingId) {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setBookingId(bookingId);
        return dto;
    }

    private Session buildSession(String id, String url) {
        Session session = new Session();
        session.setId(id);
        session.setUrl(url);
        return session;
    }

    private Session buildSessionWithStatus(String status, String paymentStatus) {
        Session session = new Session();
        session.setStatus(status);
        session.setPaymentStatus(paymentStatus);
        return session;
    }

    private String paymentStatus(Long paymentId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE id = ?", String.class, paymentId);
    }

    private String bookingStatus(Long bookingId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM bookings WHERE id = ?", String.class, bookingId);
    }

    private String toJson(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }
}
