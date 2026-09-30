package mate.academy.accommodationbookingservice.service.stripe;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.model.Booking;

@RequiredArgsConstructor
@Service
public class StripeServiceImpl implements StripeService {
    private final static String BASE_URL = "https://localhost:8080";
    private final static String BOOKING_SUCCESS_URL = "/payments/success/";
    private final static String BOOKING_CANCEL_URL = "/payments/cancel/";
    private final static String ID_PARAM = "session_id";
    private final static String ID_PARAM_PLACEHOLDER = "{CHECKOUT_SESSION_ID}";
    public static final int DAYS_TO_EXPIRE = 7;
    public static final long EXPIRATION_TIME = Instant.now()
            .plus(DAYS_TO_EXPIRE, ChronoUnit.DAYS)
            .getEpochSecond();
    private final static long CENT_MULTIPLAYER = 100L;
    private final StripeClientProvider clientProvider;

    @Override
    public Session getBookingCheckOutSession(Booking booking, BigDecimal amount)
            throws StripeException {
        SessionCreateParams.LineItem.PriceData priceData = SessionCreateParams.LineItem.PriceData
                .builder()
                .setCurrency("USD")
                .setUnitAmount(amount.longValue() * CENT_MULTIPLAYER)
                .setProductData(
                        SessionCreateParams.LineItem.PriceData.ProductData
                                .builder()
                                .setName("Booking")
                                .build()
                )
                .build();

        String successUrl = UriComponentsBuilder
                .fromUriString(BASE_URL)
                .path(BOOKING_SUCCESS_URL)
                .queryParam(ID_PARAM, ID_PARAM_PLACEHOLDER)
                .build()
                .toUriString();

        String cancelUrl = UriComponentsBuilder
                .fromUriString(BASE_URL)
                .path(BOOKING_CANCEL_URL)
                .queryParam(ID_PARAM, ID_PARAM_PLACEHOLDER)
                .build()
                .toUriString();

        SessionCreateParams params = SessionCreateParams.builder()
                        .addLineItem(
                                SessionCreateParams.LineItem
                                        .builder()
                                        .setQuantity(1L)
                                        .setPriceData(priceData)
                                        .build()
                        )
                        .setExpiresAt(EXPIRATION_TIME)
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .build();

        return clientProvider.getClient()
                .v1()
                .checkout()
                .sessions()
                .create(params);
    }
}
