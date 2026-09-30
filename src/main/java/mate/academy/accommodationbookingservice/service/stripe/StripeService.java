package mate.academy.accommodationbookingservice.service.stripe;

import java.math.BigDecimal;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

public interface StripeService {
    Session getBookingCheckOutSession(BigDecimal unitAmount
    ) throws StripeException;

    Session retrieveSession(String sessionId) throws StripeException;
}
