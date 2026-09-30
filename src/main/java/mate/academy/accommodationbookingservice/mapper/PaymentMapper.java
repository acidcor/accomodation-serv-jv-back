package mate.academy.accommodationbookingservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import mate.academy.accommodationbookingservice.config.MapperConfig;
import mate.academy.accommodationbookingservice.dto.payment.PaymentRedirectionResponse;
import mate.academy.accommodationbookingservice.dto.payment.PaymentResponseDto;
import mate.academy.accommodationbookingservice.model.Payment;

@Mapper(config = MapperConfig.class)
public interface PaymentMapper {
    @Mapping(
            target = "sessionUrl",
            expression = "java(URI.create(payment.getSessionUrl()))"
    )
    PaymentResponseDto toDto (Payment payment);

    @Mapping(
            target = "sessionUrl",
            expression = "java(URI.create(payment.getSessionUrl()))"
    )
    PaymentRedirectionResponse toRedirectDto (Payment payment);
}
