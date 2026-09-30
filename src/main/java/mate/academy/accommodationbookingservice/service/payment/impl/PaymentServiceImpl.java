package mate.academy.accommodationbookingservice.service.payment.impl;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.payment.PaymentRedirectionResponse;
import mate.academy.accommodationbookingservice.dto.payment.PaymentRequestDto;
import mate.academy.accommodationbookingservice.dto.payment.PaymentResponseDto;
import mate.academy.accommodationbookingservice.dto.stripe.StripeRequestDto;
import mate.academy.accommodationbookingservice.exception.PaymentInitializationException;
import mate.academy.accommodationbookingservice.exception.PaymentNotFoundException;
import mate.academy.accommodationbookingservice.exception.PaymentSessionException;
import mate.academy.accommodationbookingservice.mapper.PaymentMapper;
import mate.academy.accommodationbookingservice.model.Booking;
import mate.academy.accommodationbookingservice.model.BookingStatus;
import mate.academy.accommodationbookingservice.model.Payment;
import mate.academy.accommodationbookingservice.model.PaymentStatus;
import mate.academy.accommodationbookingservice.model.Role;
import mate.academy.accommodationbookingservice.model.User;
import mate.academy.accommodationbookingservice.repository.BookingRepository;
import mate.academy.accommodationbookingservice.repository.PaymentRepository;
import mate.academy.accommodationbookingservice.service.AuthenticatedUserProvider;
import mate.academy.accommodationbookingservice.service.payment.PaymentService;
import mate.academy.accommodationbookingservice.service.stripe.StripeService;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService, AuthenticatedUserProvider {
    public static final String SESSION_PAYMENT_PAID = "paid";

    public static final String SESSION_PAYMENT_UNPAID = "unpaid";

    public static final String SESSION_PAYMENT_NO_PAYMENT_REQUIRED = "no_payment_required";

    public static final String SESSION_EXPIRED = "expired";

    public static final String SESSION_OPEN = "open";

    public static final String SESSION_COMPLETE = "complete";

    private final BookingRepository bookingRepository;

    private final StripeService stripeService;

    private final PaymentMapper paymentMapper;

    private final PaymentRepository paymentRepository;

    @Override
    public Page<PaymentResponseDto> findAll(Authentication authentication, Pageable pageable) {
        User user = getUserFromAuth(authentication);
        if (user
                .getRoles()
                .stream()
                .anyMatch(role -> role.getName() == Role.ADMIN)) {
            return paymentRepository
                    .findAll(pageable)
                    .map(paymentMapper::toDto);
        }
        return paymentRepository
                .findPaymentsByBookingUser(user, pageable)
                .map(paymentMapper::toDto);
    }

    @Override
    public PaymentRedirectionResponse initPayment(
            PaymentRequestDto request,
            Authentication authentication
    ) {
        User user = getUserFromAuth(authentication);
        Long bookingId = request.getBookingId();
        Booking booking = bookingRepository
                .findBookingByIdAndUser(bookingId, user)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Can't find booking with such id " + bookingId)
                );
        checkBookingStatus(booking.getStatus());
        long daysAmount = ChronoUnit.DAYS.between(
                booking.getCheckIn(), booking.getCheckOut());
        BigDecimal amount = BigDecimal
                .valueOf(daysAmount)
                .multiply(booking
                        .getAccommodation()
                        .getDailyRate());
        Session session = null;
        try {
            session = stripeService.getBookingCheckOutSession(amount);
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setStatus(PaymentStatus.AWAIT_PAYMENT);
        payment.setAmount(amount);
        payment.setSessionUrl(session.getUrl());
        payment.setSessionId(session.getId());

        return paymentMapper.toRedirectDto(paymentRepository.save(payment));
    }

    @Override
    public PaymentResponseDto getSuccess(Authentication authentication,
                                         String sessionId) {
        User user = getUserFromAuth(authentication);
        Payment payment = paymentRepository
                .findPaymentBySessionIdAndBookingUser(sessionId, user)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Can't find payment by Id: " + sessionId)
                );
        return paymentMapper.toDto(payment);
    }

    @Override
    public PaymentResponseDto getCancel(Authentication authentication, String sessionId) {
        User user = getUserFromAuth(authentication);
        Payment payment = paymentRepository
                .findPaymentBySessionIdAndBookingUser(sessionId, user)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Can't find payment by Id: " + sessionId)
                );
        return paymentMapper.toDto(payment);
    }

    @Override
    public void handleStripeCompleteRequest(StripeRequestDto requestDto) {
        String sessionId = requestDto.getId();
        Payment payment = paymentRepository
                .findPaymentBySessionId(sessionId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Can't find payment by Id: " + sessionId));
        String status = requestDto.getStatus();
        if (SESSION_PAYMENT_PAID.equals(status)) {
            payment.setStatus(PaymentStatus.PAID);
            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            paymentRepository.save(payment);
        }
    }

    @Override
    public void handleStripeExpiredRequest(StripeRequestDto requestDto) {
        String sessionId = requestDto.getId();
        Payment payment = paymentRepository
                .findPaymentBySessionId(sessionId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Can't find payment by session Id: " + sessionId));
        payment.setStatus(PaymentStatus.EXPIRED);
        paymentRepository.save(payment);
    }

    @Override
    public void syncPayment(Long id) {
        Payment payment = paymentRepository
                .findById(id)
                .orElseThrow(
                        () -> new PaymentNotFoundException(
                                "Can't find payment by Id: " + id)
                );
        syncStatus(payment);
        paymentRepository.save(payment);
    }

    @Override
    public void syncPayments() {
        List<Payment> paymentsList = paymentRepository
                .findPaymentsByStatus(PaymentStatus.AWAIT_PAYMENT);
        for (Payment payment : paymentsList) {
            try {
                syncStatus(payment);
                paymentRepository.save(payment);
            } catch (Exception e) {
                // Continue syncing other payments if one fails
            }
        }
    }

    private void syncStatus(Payment payment) {
        String sessionId = payment.getSessionId();
        Session session;
        try {
            session = stripeService.retrieveSession(sessionId);
        } catch (StripeException e) {
            throw new PaymentSessionException(
                    "Can't retrieve payment session by session Id: " + sessionId
            );
        }
        PaymentStatus status = handleStatus(session);
        if (status == PaymentStatus.PAID) {
            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        }
        payment.setStatus(status);
    }

    private PaymentStatus handleStatus(Session session) {
        String sessionStatus = session.getStatus();
        if (SESSION_EXPIRED.equals(sessionStatus)) {
            return PaymentStatus.EXPIRED;
        }
        if (SESSION_OPEN.equals(sessionStatus)) {
            return PaymentStatus.AWAIT_PAYMENT;
        }
        if (SESSION_COMPLETE.equals(sessionStatus)) {
            String paymentStatus = session.getPaymentStatus();
            if (SESSION_PAYMENT_PAID.equals(paymentStatus)
                    || SESSION_PAYMENT_NO_PAYMENT_REQUIRED.equals(paymentStatus)) {
                return PaymentStatus.PAID;
            }
            if (SESSION_PAYMENT_UNPAID.equals(paymentStatus)) {
                return PaymentStatus.AWAIT_PAYMENT;
            }
        }
        throw new PaymentSessionException(String.format(
                "Can't find suitable status for session Id: %s with status %s",
                session.getId(),
                sessionStatus)
        );
    }

    private void checkBookingStatus(BookingStatus status) {
        if (status != BookingStatus.AWAIT_PAYMENT) {
            throw new PaymentInitializationException(
                    "Can't init payment session for booking status: " + status
            );
        }
    }
}
