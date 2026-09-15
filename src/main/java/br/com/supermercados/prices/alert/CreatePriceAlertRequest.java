package br.com.supermercados.prices.alert;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePriceAlertRequest(
        @NotNull UUID productId,
        @NotNull UUID cityId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal targetPrice) {
}
