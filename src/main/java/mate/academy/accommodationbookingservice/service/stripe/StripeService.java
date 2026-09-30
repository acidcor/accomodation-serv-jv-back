package mate.academy.accommodationbookingservice.service.stripe;

import java.math.BigDecimal;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import mate.academy.accommodationbookingservice.model.Booking;

public interface StripeService {
    Session getBookingCheckOutSession(Booking booking,
                                      BigDecimal unitAmount
    ) throws StripeException;
}
