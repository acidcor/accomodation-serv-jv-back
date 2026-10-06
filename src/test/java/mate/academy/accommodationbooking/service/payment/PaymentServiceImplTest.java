package mate.academy.accommodationbooking.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.stripe.exception.ApiException;
import com.stripe.model.checkout.Session;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import mate.academy.accommodationbooking.service.dto.payment.PaymentCancelingResponseDto;
import mate.academy.accommodationbooking.service.dto.payment.PaymentRedirectionResponse;
import mate.academy.accommodationbooking.service.dto.payment.PaymentRequestDto;
import mate.academy.accommodationbooking.service.dto.payment.PaymentResponseDto;
import mate.academy.accommodationbooking.service.dto.stripe.StripeRequestDto;
import mate.academy.accommodationbooking.service.exception.notfound.EntityNotFoundException;
import mate.academy.accommodationbooking.service.exception.stripe.PaymentSessionException;
import mate.academy.accommodationbooking.service.exception.validation.PaymentInitializationException;
import mate.academy.accommodationbooking.service.mapper.PaymentMapper;
import mate.academy.accommodationbooking.service.model.Accommodation;
import mate.academy.accommodationbooking.service.model.Booking;
import mate.academy.accommodationbooking.service.model.BookingStatus;
import mate.academy.accommodationbooking.service.model.Payment;
import mate.academy.accommodationbooking.service.model.PaymentStatus;
import mate.academy.accommodationbooking.service.model.Role;
import mate.academy.accommodationbooking.service.model.RoleEntity;
import mate.academy.accommodationbooking.service.model.User;
import mate.academy.accommodationbooking.service.repository.BookingRepository;
import mate.academy.accommodationbooking.service.repository.PaymentRepository;
import mate.academy.accommodationbooking.service.service.payment.impl.PaymentServiceImpl;
import mate.academy.accommodationbooking.service.service.stripe.StripeService;
import mate.academy.accommodationbooking.service.service.telegram.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    private static final String EMAIL = "user@example.com";
    private static final Long BOOKING_ID = 10L;
    private static final Long PAYMENT_ID = 20L;
    private static final String SESSION_ID = "sess_old";
    private static final String NEW_SESSION_ID = "sess_new";
    private static final String NEW_SESSION_URL = "https://checkout.stripe.test/new";
    private static final BigDecimal DAILY_RATE = new BigDecimal("150.00");
    private static final BigDecimal AMOUNT = new BigDecimal("750.00"); // 5 nights * 150.00

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private StripeService stripeService;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private Session session;
    @Mock
    private NotificationService notificationService;
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("Find all: admin sees payments of all users")
    void findAll_admin_returnsAllPayments() {
        // given
        User admin = buildUser(Role.ADMIN);
        Pageable pageable = PageRequest.of(0, 10);
        Payment payment = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        PaymentResponseDto dto = buildDto();

        givenAuthenticatedUser(admin);
        given(paymentRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(payment)));
        given(paymentMapper.toDto(payment)).willReturn(dto);

        // when
        Page<PaymentResponseDto> actual = paymentService.findAll(authentication, pageable);

        // then
        assertEquals(List.of(dto), actual.getContent());
        then(paymentRepository).should(never()).findPaymentsByBookingUser(any(), any());
    }

    @Test
    @DisplayName("Find all: customer sees only their own payments")
    void findAll_customer_returnsOnlyOwnPayments() {
        // given
        User customer = buildUser(Role.CUSTOMER);
        Pageable pageable = PageRequest.of(0, 10);
        Payment payment = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        PaymentResponseDto dto = buildDto();

        givenAuthenticatedUser(customer);
        given(paymentRepository.findPaymentsByBookingUser(customer, pageable))
                .willReturn(new PageImpl<>(List.of(payment)));
        given(paymentMapper.toDto(payment)).willReturn(dto);

        // when
        Page<PaymentResponseDto> actual = paymentService.findAll(authentication, pageable);

        // then
        assertEquals(List.of(dto), actual.getContent());
        then(paymentRepository).should(never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Init payment: booking awaiting payment saves a payment with the calculated amount (5 nights x daily rate)")
    void initPayment_awaitPaymentBooking_savesPaymentWithCalculatedAmount() throws Exception {
        // given
        User user = buildUser(Role.CUSTOMER);
        Booking booking = buildBooking(BookingStatus.AWAIT_PAYMENT);
        PaymentRequestDto request = buildPaymentRequest();
        PaymentRedirectionResponse expected = new PaymentRedirectionResponse();
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);

        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(stripeService.getBookingCheckOutSession(any(BigDecimal.class)))
                .willReturn(session);
        given(session.getUrl()).willReturn(NEW_SESSION_URL);
        given(session.getId()).willReturn(NEW_SESSION_ID);
        given(paymentRepository.save(any(Payment.class))).willAnswer(i -> i.getArgument(0));
        given(paymentMapper.toRedirectDto(any(Payment.class))).willReturn(expected);

        // when
        PaymentRedirectionResponse actual = paymentService.initPayment(request, authentication);

        // then
        assertSame(expected, actual);
        then(paymentRepository).should().save(captor.capture());
        Payment saved = captor.getValue();
        assertEquals(0, AMOUNT.compareTo(saved.getAmount()));
        assertEquals(PaymentStatus.AWAIT_PAYMENT, saved.getStatus());
        assertEquals(NEW_SESSION_ID, saved.getSessionId());
        assertEquals(NEW_SESSION_URL, saved.getSessionUrl());
        assertSame(booking, saved.getBooking());
    }

    @Test
    @DisplayName("Init payment: throws EntityNotFoundException when the booking is not found for the user")
    void initPayment_bookingNotFound_throwsEntityNotFoundException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.initPayment(buildPaymentRequest(), authentication));

        // then
        assertEquals("Can't find booking with such id " + BOOKING_ID, exception.getMessage());
        then(stripeService).shouldHaveNoInteractions();
        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Init payment: throws PaymentInitializationException when the booking is not awaiting payment")
    void initPayment_bookingNotAwaitingPayment_throwsPaymentInitializationException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        Booking booking = buildBooking(BookingStatus.CONFIRMED);
        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));

        // when
        PaymentInitializationException exception = assertThrows(
                PaymentInitializationException.class,
                () -> paymentService.initPayment(buildPaymentRequest(), authentication));

        // then
        assertEquals("Can't init payment session for booking status: CONFIRMED",
                exception.getMessage());
        then(stripeService).shouldHaveNoInteractions();
        then(paymentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Init payment: throws PaymentInitializationException when Stripe fails to create a session")
    void initPayment_stripeThrows_throwsPaymentInitializationException() throws Exception {
        // given
        User user = buildUser(Role.CUSTOMER);
        Booking booking = buildBooking(BookingStatus.AWAIT_PAYMENT);
        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(stripeService.getBookingCheckOutSession(any(BigDecimal.class)))
                .willThrow(new ApiException("Stripe error", null, null, 500, null));

        // when
        PaymentInitializationException exception = assertThrows(
                PaymentInitializationException.class,
                () -> paymentService.initPayment(buildPaymentRequest(), authentication));

        // then
        assertEquals("Failed to create payment session for booking ID: " + BOOKING_ID,
                exception.getMessage());
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Init payment: throws PaymentInitializationException when Stripe returns a null session")
    void initPayment_stripeReturnsNullSession_throwsPaymentInitializationException()
            throws Exception {
        // given
        User user = buildUser(Role.CUSTOMER);
        Booking booking = buildBooking(BookingStatus.AWAIT_PAYMENT);
        givenAuthenticatedUser(user);
        given(bookingRepository.findBookingByIdAndUser(BOOKING_ID, user))
                .willReturn(Optional.of(booking));
        given(stripeService.getBookingCheckOutSession(any(BigDecimal.class))).willReturn(null);

        // when
        PaymentInitializationException exception = assertThrows(
                PaymentInitializationException.class,
                () -> paymentService.initPayment(buildPaymentRequest(), authentication));

        // then
        assertEquals("Failed to create payment session for booking ID: " + BOOKING_ID,
                exception.getMessage());
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Get success: returns the payment DTO found by session ID for the user")
    void getSuccess_paymentExists_returnsDto() {
        // given
        User user = buildUser(Role.CUSTOMER);
        Payment payment = buildPayment(PaymentStatus.PAID);
        PaymentResponseDto dto = buildDto();
        givenAuthenticatedUser(user);
        given(paymentRepository.findPaymentBySessionIdAndBookingUser(SESSION_ID, user))
                .willReturn(Optional.of(payment));
        given(paymentMapper.toDto(payment)).willReturn(dto);

        // when
        PaymentResponseDto actual = paymentService.getSuccess(authentication, SESSION_ID);

        // then
        assertEquals(dto, actual);
    }

    @Test
    @DisplayName("Get success: throws EntityNotFoundException when the payment is not found")
    void getSuccess_paymentNotFound_throwsEntityNotFoundException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        givenAuthenticatedUser(user);
        given(paymentRepository.findPaymentBySessionIdAndBookingUser(SESSION_ID, user))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.getSuccess(authentication, SESSION_ID));

        // then
        assertEquals("Can't find payment by Id: " + SESSION_ID, exception.getMessage());
    }

    @Test
    @DisplayName("Get cancel: returns the payment closing DTO found by "
            + "session ID for the user with propper booking ID message")
    void getCancel_paymentExists_returnsDto() {
        // given
        User user = buildUser(Role.CUSTOMER);
        Payment payment = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        PaymentResponseDto dto = buildDto();
        givenAuthenticatedUser(user);
        given(paymentRepository.findPaymentBySessionIdAndBookingUser(SESSION_ID, user))
                .willReturn(Optional.of(payment));

        // when
        PaymentCancelingResponseDto actual = paymentService.getCancel(authentication, SESSION_ID);

        // then
        assertTrue(actual.getMessage().contains(payment.getBooking().getId().toString()));
    }

    @Test
    @DisplayName("Get cancel: throws EntityNotFoundException when the payment is not found")
    void getCancel_paymentNotFound_throwsEntityNotFoundException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        givenAuthenticatedUser(user);
        given(paymentRepository.findPaymentBySessionIdAndBookingUser(SESSION_ID, user))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.getCancel(authentication, SESSION_ID));

        // then
        assertEquals("Can't find payment by Id: " + SESSION_ID, exception.getMessage());
    }

    @Test
    @DisplayName("Stripe complete webhook: 'paid' marks the payment PAID and confirms the booking")
    void handleStripeCompleteRequest_paid_confirmsBookingAndMarksPaymentPaid() {
        // given
        Payment payment = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        given(paymentRepository.findPaymentBySessionId(SESSION_ID))
                .willReturn(Optional.of(payment));
        given(paymentRepository.save(payment)).willReturn(payment);

        // when
        paymentService.handleStripeCompleteRequest(buildStripeRequest("paid"));

        // then
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(BookingStatus.CONFIRMED, payment.getBooking().getStatus());
        then(bookingRepository).should().save(payment.getBooking());
        then(paymentRepository).should().save(payment);
        then(notificationService).should().sendPaymentConfirmed(payment);
    }

    @Test
    @DisplayName("Stripe complete webhook: 'unpaid' leaves payment and booking unchanged")
    void handleStripeCompleteRequest_unpaid_changesNothing() {
        // given
        Payment payment = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        given(paymentRepository.findPaymentBySessionId(SESSION_ID))
                .willReturn(Optional.of(payment));

        // when
        paymentService.handleStripeCompleteRequest(buildStripeRequest("unpaid"));

        // then
        assertEquals(PaymentStatus.AWAIT_PAYMENT, payment.getStatus());
        assertEquals(BookingStatus.AWAIT_PAYMENT, payment.getBooking().getStatus());
        then(bookingRepository).shouldHaveNoInteractions();
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Stripe complete webhook: throws EntityNotFoundException when the payment is not found")
    void handleStripeCompleteRequest_paymentNotFound_throwsEntityNotFoundException() {
        // given
        given(paymentRepository.findPaymentBySessionId(SESSION_ID))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.handleStripeCompleteRequest(buildStripeRequest("paid")));

        // then
        assertEquals("Can't find payment by Id: " + SESSION_ID, exception.getMessage());
    }

    @Test
    @DisplayName("Stripe expired webhook: marks the payment EXPIRED")
    void handleStripeExpiredRequest_paymentExists_marksPaymentExpired() {
        // given
        Payment payment = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        given(paymentRepository.findPaymentBySessionId(SESSION_ID))
                .willReturn(Optional.of(payment));
        given(paymentRepository.save(payment)).willReturn(payment);

        // when
        paymentService.handleStripeExpiredRequest(buildStripeRequest("expired"));

        // then
        // then
        assertEquals(PaymentStatus.EXPIRED, payment.getStatus());
        then(paymentRepository).should().save(payment);
        then(notificationService).should().sendPaymentExpired(payment);
    }

    @Test
    @DisplayName("Stripe expired webhook: throws EntityNotFoundException when the payment is not found")
    void handleStripeExpiredRequest_paymentNotFound_throwsEntityNotFoundException() {
        // given
        given(paymentRepository.findPaymentBySessionId(SESSION_ID))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.handleStripeExpiredRequest(buildStripeRequest("expired")));

        // then
        assertEquals("Can't find payment by session Id: " + SESSION_ID, exception.getMessage());
    }

    @Test
    @DisplayName("Sync payment: complete and paid session marks payment PAID and confirms the booking")
    void syncPayment_sessionCompleteAndPaid_marksPaidAndConfirmsBooking() throws Exception {
        // given
        Payment payment = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(stripeService.retrieveSession(SESSION_ID)).willReturn(session);
        given(session.getStatus()).willReturn("complete");
        given(session.getPaymentStatus()).willReturn("paid");

        // when
        paymentService.syncPayment(PAYMENT_ID);

        // then
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(BookingStatus.CONFIRMED, payment.getBooking().getStatus());
        then(bookingRepository).should().save(payment.getBooking());
        then(paymentRepository).should().save(payment);
    }

    @Test
    @DisplayName("Sync payment: open session keeps AWAIT_PAYMENT and does not touch the booking")
    void syncPayment_sessionOpen_keepsAwaitPaymentAndDoesNotTouchBooking() throws Exception {
        // given
        Payment payment = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(stripeService.retrieveSession(SESSION_ID)).willReturn(session);
        given(session.getStatus()).willReturn("open");

        // when
        paymentService.syncPayment(PAYMENT_ID);

        // then
        assertEquals(PaymentStatus.AWAIT_PAYMENT, payment.getStatus());
        assertEquals(BookingStatus.AWAIT_PAYMENT, payment.getBooking().getStatus());
        then(bookingRepository).shouldHaveNoInteractions();
        then(paymentRepository).should().save(payment);
    }

    @Test
    @DisplayName("Sync payment: expired session marks the payment EXPIRED")
    void syncPayment_sessionExpired_marksPaymentExpired() throws Exception {
        // given
        Payment payment = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(stripeService.retrieveSession(SESSION_ID)).willReturn(session);
        given(session.getStatus()).willReturn("expired");

        // when
        paymentService.syncPayment(PAYMENT_ID);

        // then
        assertEquals(PaymentStatus.EXPIRED, payment.getStatus());
        then(paymentRepository).should().save(payment);
    }

    @Test
    @DisplayName("Sync payment: throws EntityNotFoundException and skips Stripe when the payment is not found")
    void syncPayment_paymentNotFound_throwsEntityNotFoundException() {
        // given
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.syncPayment(PAYMENT_ID));

        // then
        assertEquals("Can't find payment by Id: " + PAYMENT_ID, exception.getMessage());
        then(stripeService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Sync payment: throws PaymentSessionException when retrieving the Stripe session fails")
    void syncPayment_stripeRetrieveFails_throwsPaymentSessionException() throws Exception {
        // given
        Payment payment = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(stripeService.retrieveSession(SESSION_ID))
                .willThrow(new ApiException("Stripe error", null, null, 500, null));

        // when
        PaymentSessionException exception = assertThrows(PaymentSessionException.class,
                () -> paymentService.syncPayment(PAYMENT_ID));

        // then
        assertEquals("Can't retrieve payment session by session Id: " + SESSION_ID,
                exception.getMessage());
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Sync payment: throws PaymentSessionException for an unknown session status")
    void syncPayment_unknownSessionStatus_throwsPaymentSessionException() throws Exception {
        // given
        Payment payment = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(stripeService.retrieveSession(SESSION_ID)).willReturn(session);
        given(session.getStatus()).willReturn("unknown_status");

        // when
        PaymentSessionException exception = assertThrows(PaymentSessionException.class,
                () -> paymentService.syncPayment(PAYMENT_ID));

        // then
        assertTrue(exception.getMessage().contains("unknown_status"));
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Sync payments: a failure on one payment does not stop the others from being synced")
    void syncPayments_oneFailsOneSucceeds_failureDoesNotStopTheRest() throws Exception {
        // given
        Payment failing = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        failing.setSessionId("sess_failing");
        Payment succeeding = buildPayment(PaymentStatus.AWAIT_PAYMENT);
        succeeding.setSessionId("sess_ok");

        given(paymentRepository.findPaymentsByStatus(PaymentStatus.AWAIT_PAYMENT))
                .willReturn(List.of(failing, succeeding));
        given(stripeService.retrieveSession("sess_failing"))
                .willThrow(new ApiException("Stripe error", null, null, 500, null));
        given(stripeService.retrieveSession("sess_ok")).willReturn(session);
        given(session.getStatus()).willReturn("expired");

        // when
        paymentService.syncPayments();

        // then
        assertEquals(PaymentStatus.AWAIT_PAYMENT, failing.getStatus());
        assertEquals(PaymentStatus.EXPIRED, succeeding.getStatus());
        then(paymentRepository).should(never()).save(failing);
        then(paymentRepository).should().save(succeeding);
    }

    @Test
    @DisplayName("Sync payments: no awaiting payments means no Stripe calls and nothing saved")
    void syncPayments_noAwaitingPayments_doesNotCallStripe() {
        // given
        given(paymentRepository.findPaymentsByStatus(PaymentStatus.AWAIT_PAYMENT))
                .willReturn(List.of());

        // when
        paymentService.syncPayments();

        // then
        then(stripeService).shouldHaveNoInteractions();
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @ParameterizedTest
    @DisplayName("Renew payment: EXPIRED or AWAIT_PAYMENT payment gets a new session and becomes AWAIT_PAYMENT")
    @EnumSource(value = PaymentStatus.class, names = {"EXPIRED", "AWAIT_PAYMENT"})
    void renewPayment_renewableStatus_setsNewSessionAndAwaitPayment(PaymentStatus status)
            throws Exception {
        // given
        User user = buildUser(Role.CUSTOMER);
        Payment payment = buildPayment(status);
        PaymentRedirectionResponse expected = new PaymentRedirectionResponse();

        givenAuthenticatedUser(user);
        given(paymentRepository.findByIdAndBookingUser(PAYMENT_ID, user))
                .willReturn(Optional.of(payment));
        given(stripeService.getBookingCheckOutSession(AMOUNT)).willReturn(session);
        given(session.getUrl()).willReturn(NEW_SESSION_URL);
        given(session.getId()).willReturn(NEW_SESSION_ID);
        given(paymentRepository.save(payment)).willReturn(payment);
        given(paymentMapper.toRedirectDto(payment)).willReturn(expected);

        // when
        PaymentRedirectionResponse actual = paymentService.renewPayment(PAYMENT_ID,
                authentication);

        // then
        assertSame(expected, actual);
        assertEquals(PaymentStatus.AWAIT_PAYMENT, payment.getStatus());
        assertEquals(NEW_SESSION_ID, payment.getSessionId());
        assertEquals(NEW_SESSION_URL, payment.getSessionUrl());
    }

    @Test
    @DisplayName("Renew payment: throws PaymentInitializationException for an already PAID payment")
    void renewPayment_paidPayment_throwsPaymentInitializationException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        Payment payment = buildPayment(PaymentStatus.PAID);
        givenAuthenticatedUser(user);
        given(paymentRepository.findByIdAndBookingUser(PAYMENT_ID, user))
                .willReturn(Optional.of(payment));

        // when
        PaymentInitializationException exception = assertThrows(
                PaymentInitializationException.class,
                () -> paymentService.renewPayment(PAYMENT_ID, authentication));

        // then
        assertEquals("Can't renew payment with status: PAID", exception.getMessage());
        then(stripeService).shouldHaveNoInteractions();
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Renew payment: throws EntityNotFoundException when the payment is not found for the user")
    void renewPayment_paymentNotFound_throwsEntityNotFoundException() {
        // given
        User user = buildUser(Role.CUSTOMER);
        givenAuthenticatedUser(user);
        given(paymentRepository.findByIdAndBookingUser(PAYMENT_ID, user))
                .willReturn(Optional.empty());

        // when
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.renewPayment(PAYMENT_ID, authentication));

        // then
        assertEquals(String.format("Can't find payment by ID '%s' related to user: %s",
                PAYMENT_ID, EMAIL), exception.getMessage());
    }

    @Test
    @DisplayName("Renew payment: Stripe failure throws and leaves the payment unchanged")
    void renewPayment_stripeThrows_throwsAndKeepsPaymentUnchanged() throws Exception {
        // given
        User user = buildUser(Role.CUSTOMER);
        Payment payment = buildPayment(PaymentStatus.EXPIRED);
        givenAuthenticatedUser(user);
        given(paymentRepository.findByIdAndBookingUser(PAYMENT_ID, user))
                .willReturn(Optional.of(payment));
        given(stripeService.getBookingCheckOutSession(AMOUNT))
                .willThrow(new ApiException("Stripe error", null, null, 500, null));

        // when
        PaymentInitializationException exception = assertThrows(
                PaymentInitializationException.class,
                () -> paymentService.renewPayment(PAYMENT_ID, authentication));

        // then
        assertEquals("Failed to create payment session for booking ID: " + BOOKING_ID,
                exception.getMessage());
        assertEquals(PaymentStatus.EXPIRED, payment.getStatus());
        assertEquals(SESSION_ID, payment.getSessionId());
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Renew payment: null Stripe session throws PaymentInitializationException")
    void renewPayment_stripeReturnsNullSession_throwsPaymentInitializationException()
            throws Exception {
        // given
        User user = buildUser(Role.CUSTOMER);
        Payment payment = buildPayment(PaymentStatus.EXPIRED);
        givenAuthenticatedUser(user);
        given(paymentRepository.findByIdAndBookingUser(PAYMENT_ID, user))
                .willReturn(Optional.of(payment));
        given(stripeService.getBookingCheckOutSession(AMOUNT)).willReturn(null);

        // when
        PaymentInitializationException exception = assertThrows(
                PaymentInitializationException.class,
                () -> paymentService.renewPayment(PAYMENT_ID, authentication));

        // then
        assertEquals("Failed to create payment session for booking ID: " + BOOKING_ID,
                exception.getMessage());
        then(paymentRepository).should(never()).save(any(Payment.class));
    }

    private void givenAuthenticatedUser(User user) {
        given(authentication.getPrincipal()).willReturn(user);
    }

    private User buildUser(Role role) {
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(1L);
        roleEntity.setName(role);

        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        user.setRoles(Set.of(roleEntity));
        return user;
    }

    private Booking buildBooking(BookingStatus status) {
        Accommodation accommodation = new Accommodation();
        accommodation.setDailyRate(DAILY_RATE);

        Booking booking = new Booking();
        booking.setId(BOOKING_ID);
        booking.setCheckIn(LocalDate.of(2026, 12, 10));
        booking.setCheckOut(LocalDate.of(2026, 12, 15));
        booking.setAccommodation(accommodation);
        booking.setStatus(status);
        return booking;
    }

    private Payment buildPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setStatus(status);
        payment.setBooking(buildBooking(BookingStatus.AWAIT_PAYMENT));
        payment.setSessionId(SESSION_ID);
        payment.setSessionUrl("https://checkout.stripe.test/old");
        payment.setAmount(AMOUNT);
        return payment;
    }

    private PaymentRequestDto buildPaymentRequest() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setBookingId(BOOKING_ID);
        return request;
    }

    private StripeRequestDto buildStripeRequest(String status) {
        StripeRequestDto request = new StripeRequestDto();
        request.setId(SESSION_ID);
        request.setStatus(status);
        return request;
    }

    private PaymentResponseDto buildDto() {
        return new PaymentResponseDto(PAYMENT_ID, "AWAIT_PAYMENT", null,
                URI.create("https://checkout.stripe.test/old"), AMOUNT);
    }
}
