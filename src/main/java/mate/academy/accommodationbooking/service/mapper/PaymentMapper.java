package mate.academy.accommodationbooking.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import mate.academy.accommodationbooking.service.config.MapperConfig;
import mate.academy.accommodationbooking.service.dto.payment.PaymentRedirectionResponse;
import mate.academy.accommodationbooking.service.dto.payment.PaymentResponseDto;
import mate.academy.accommodationbooking.service.model.Payment;

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
