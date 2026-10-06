package mate.academy.accommodationbookingservice.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import mate.academy.accommodationbookingservice.exception.booking.BookingCancelingException;
import mate.academy.accommodationbookingservice.exception.notfound.EntityNotFoundException;
import mate.academy.accommodationbookingservice.exception.stripe.PaymentSessionException;
import mate.academy.accommodationbookingservice.exception.stripe.StripeException;
import mate.academy.accommodationbookingservice.exception.validation.PaymentInitializationException;
import mate.academy.accommodationbookingservice.exception.validation.RegistrationException;
import mate.academy.accommodationbookingservice.exception.validation.ValidationException;

@RestControllerAdvice
public class CustomGlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        List<String> errors = ex
                .getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());
        body.put("errors", errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleInnerValidationExceptions(ValidationException ex) {
        Map<String, Object> body = makeBody(ex);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BookingCancelingException.class)
    public ResponseEntity<Object> handleBookingCancelingException(BookingCancelingException ex) {
        Map<String, Object> body = makeBody(ex);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundExc(EntityNotFoundException ex) {
        Map<String, Object> body = makeBody(ex);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<Object> handleStripeException(StripeException ex) {
        Map<String, Object> body = makeBody(ex);
        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(PaymentSessionException.class)
    public ResponseEntity<Object> handlePaymentSessionException(PaymentSessionException ex) {
        Map<String, Object> body = makeBody(ex);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<Object> handleRegistrationException(RegistrationException ex) {
        return new ResponseEntity<>(makeBody(ex), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PaymentInitializationException.class)
    public ResponseEntity<Object> handlePaymentInitializationException(
            PaymentInitializationException ex) {
        return new ResponseEntity<>(makeBody(ex), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", "error");
        body.put("message", "Internal server error");
        body.put("errorCode", "INTERNAL_ERROR");
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, Object> makeBody(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", "error");
        body.put("message", ex.getMessage());
        body.put("errorCode", getErrorCode(ex));
        return body;
    }

    private String getErrorCode(Exception ex) {
        if (ex instanceof EntityNotFoundException) {
            return "ENTITY_NOT_FOUND";
        }
        if (ex instanceof ValidationException) {
            return "VALIDATION_ERROR";
        }
        if (ex instanceof BookingCancelingException) {
            return "BOOKING_CONFLICT";
        }
        if (ex instanceof StripeException) {
            return "PAYMENT_ERROR";
        }
        return "INTERNAL_ERROR";
    }
}
