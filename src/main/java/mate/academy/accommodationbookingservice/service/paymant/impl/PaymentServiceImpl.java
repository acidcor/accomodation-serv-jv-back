package mate.academy.accommodationbookingservice.service.paymant.impl;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
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
import mate.academy.accommodationbookingservice.exception.PaymentNotFoundException;
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
import mate.academy.accommodationbookingservice.service.paymant.PaymentService;
import mate.academy.accommodationbookingservice.service.stripe.StripeService;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService, AuthenticatedUserProvider {
    public static final String PAID_STATUS = "paid";

    private final BookingRepository bookingRepository;
    private final StripeService stripeService;
    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;

    @Override
    public Page<PaymentResponseDto> finaAll(Authentication authentication, Pageable pageable) {
        User user = getUserFromAuth(authentication);
        if (user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.ADMIN)) {
            return paymentRepository.findAll(pageable).map(paymentMapper::toDto);
        }
        return paymentRepository.findPaymentsByBookingUser(user, pageable)
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
        long daysAmount = ChronoUnit.DAYS.between(
                booking.getCheckIn(), booking.getCheckOut());
        BigDecimal amount = BigDecimal.valueOf(daysAmount)
                .multiply(booking.getAccommodation().getDailyRate());
        Session session = null;
        try {
            session = stripeService.getBookingCheckOutSession(booking, amount);
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
        Payment payment = paymentRepository.findPaymentBySessionIdAndBookingUser(sessionId, user)
                .orElseThrow(() -> new PaymentNotFoundException(
                                        "Can't find payment by Id: " + sessionId)
        );
        return paymentMapper.toDto(payment);
    }

    @Override
    public PaymentResponseDto getCancel(Authentication authentication, String sessionId) {
        User user = getUserFromAuth(authentication);
        Payment payment = paymentRepository.findPaymentBySessionIdAndBookingUser(sessionId, user)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Can't find payment by Id: " + sessionId)
                );
        return paymentMapper.toDto(payment);
    }

    @Override
    public void handleStripeCompleteRequest(StripeRequestDto requestDto) {
        String sessionId = requestDto.getId();
        Payment payment = paymentRepository.findPaymentBySessionId(sessionId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Can't find payment by Id: " + sessionId));
        String status = requestDto.getStatus();
        if (status.equals(PAID_STATUS)) {
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
        Payment payment = paymentRepository.findPaymentBySessionId(sessionId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Can't find payment by Id: " + sessionId));
        payment.setStatus(PaymentStatus.EXPIRED);
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.PENDING);
        bookingRepository.save(booking);
        paymentRepository.save(payment);
    }
}
