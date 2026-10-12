package mate.academy.accommodationbooking.service.dto.payment;

import java.net.URI;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRedirectionResponse {
    private URI sessionUrl;
}
