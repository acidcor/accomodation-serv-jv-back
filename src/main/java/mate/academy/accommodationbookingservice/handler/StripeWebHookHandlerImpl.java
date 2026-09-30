package mate.academy.accommodationbookingservice.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import mate.academy.accommodationbookingservice.dto.stripe.StripeRequestDto;
import mate.academy.accommodationbookingservice.exception.StripeDataObjectException;
import mate.academy.accommodationbookingservice.exception.StripeEventException;
import mate.academy.accommodationbookingservice.service.payment.PaymentService;
import mate.academy.accommodationbookingservice.service.stripe.StripeClientProvider;

@RequiredArgsConstructor
@Service
public class StripeWebHookHandlerImpl implements StripeWebHookHandler {
    @Value("${sk.stripe.webhook}")
    private String endpointSecret;

    private final PaymentService paymentService;

    private final StripeClientProvider clientProvider;

    @Override
    public void handle(String payload, String signature) {
        Event event = null;
        if (endpointSecret != null && !endpointSecret.isEmpty()
                && signature != null && !signature.isEmpty()) {
            try {
                event = clientProvider
                        .getClient()
                        .constructEvent(
                                payload, signature, endpointSecret
                        );
            } catch (SignatureVerificationException e) {
                throw new StripeEventException(
                        "Can't create an event through signature verification"
                );
            }
        } else {
            event = Event.GSON.fromJson(payload, Event.class);
        }
        if (event == null) {
            throw new StripeEventException("Event object can't be null");
        }
        StripeObject stripeObject = getStripeObject(event);
        switch (event.getType()) {
            case "checkout.session.completed": {
                completeHandler(stripeObject);
                break;
            }
            case "checkout.session.expired": {
                expiredHandler(stripeObject);
                break;
            }
            default: {
                System.out.println("Unhandled event type: " + event.getType());
            }

        }
    }

    private static StripeObject getStripeObject(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        return dataObjectDeserializer
                .getObject()
                .orElseThrow(() -> new StripeDataObjectException(
                        "Can't deserialize a stripe object due to version incompatibility"
                ));
    }

    private void completeHandler(StripeObject stripeObject) {
        paymentService.handleStripeCompleteRequest(getDtoFromObject(stripeObject));
    }

    private void expiredHandler(StripeObject stripeObject) {
        paymentService.handleStripeExpiredRequest(getDtoFromObject(stripeObject));
    }

    private StripeRequestDto getDtoFromObject(StripeObject stripeObject) {
        StripeRequestDto dto = new StripeRequestDto();
        Session session = (Session) stripeObject;
        dto.setId(session.getId());
        dto.setStatus(session.getPaymentStatus());
        return dto;
    }
}