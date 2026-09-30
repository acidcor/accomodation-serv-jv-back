package mate.academy.accommodationbookingservice.service.stripe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.stripe.StripeClient;
import lombok.Getter;

@Getter
@Component
public class StripeClientProvider {
    private final StripeClient client;

    public StripeClientProvider(@Value("${sk.stripe}") String skStripe) {
        this.client = new StripeClient(skStripe);
    }
}

