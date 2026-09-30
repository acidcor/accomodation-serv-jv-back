package mate.academy.accommodationbookingservice.service.stripe;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class StripeServiceImpl implements StripeService {
    @Value("${base.url}")
    private String baseUrl;
    private final static String BOOKING_SUCCESS_URL = "/payments/success";
    private final static String BOOKING_CANCEL_URL = "/payments/cancel";
    private final static String ID_PARAM = "session_id";
    private final static String ID_PARAM_PLACEHOLDER = "{CHECKOUT_SESSION_ID}";
    private final static String ITEM_NAME = "Booking";
    private final static String BASE_CURRENCY = "USD";
    public static final int HOURS_TO_EXPIRE = 24;
    private final static BigDecimal CENT_MULTIPLIER = BigDecimal.valueOf(100);
    private final StripeClientProvider clientProvider;

    @Override
    public Session getBookingCheckOutSession(BigDecimal amount)
            throws StripeException {
        SessionCreateParams.LineItem.PriceData priceData = SessionCreateParams.LineItem.PriceData
                .builder()
                .setCurrency(BASE_CURRENCY)
                .setUnitAmount(amount.multiply(CENT_MULTIPLIER).longValue())
                .setProductData(
                        SessionCreateParams.LineItem.PriceData.ProductData
                                .builder()
                                .setName(ITEM_NAME)
                                .build()
                )
                .build();

        String successUrl = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(BOOKING_SUCCESS_URL)
                .queryParam(ID_PARAM, ID_PARAM_PLACEHOLDER)
                .build()
                .toUriString();

        String cancelUrl = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path(BOOKING_CANCEL_URL)
                .queryParam(ID_PARAM, ID_PARAM_PLACEHOLDER)
                .build()
                .toUriString();
        long expirationDate = Instant
                .now()
                .plus(HOURS_TO_EXPIRE, ChronoUnit.HOURS)
                .getEpochSecond();
        SessionCreateParams params = SessionCreateParams.builder()
                        .addLineItem(
                                SessionCreateParams.LineItem
                                        .builder()
                                        .setQuantity(1L)
                                        .setPriceData(priceData)
                                        .build()
                        )
                        .setExpiresAt(expirationDate)
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

    @Override
    public Session retrieveSession(String sessionId) throws StripeException {
        SessionRetrieveParams params = SessionRetrieveParams.builder().build();
        return clientProvider.getClient()
                .v1()
                .checkout()
                .sessions()
                .retrieve(sessionId, params);
    }
}
