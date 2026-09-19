package br.com.supermercados.prices.price;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MeasurementPriceTest {

    @Test
    void calculatesLitresKilogramsAndPackageUnitsWithoutChangingPackagePrice() {
        assertThat(MeasurementPrice.calculate(new BigDecimal("12.50"), new BigDecimal("2500"), "ML"))
                .isEqualTo(new MeasurementPrice(new BigDecimal("5.0000"), "L"));
        assertThat(MeasurementPrice.calculate(new BigDecimal("10"), new BigDecimal("500"), "G"))
                .isEqualTo(new MeasurementPrice(new BigDecimal("20.0000"), "KG"));
        assertThat(MeasurementPrice.calculate(new BigDecimal("24"), new BigDecimal("12"), "UN"))
                .isEqualTo(new MeasurementPrice(new BigDecimal("2.0000"), "UN"));
        assertThat(MeasurementPrice.calculate(new BigDecimal("3.50"), new BigDecimal("350"), "ML").amount())
                .isEqualByComparingTo("10");
    }

    @Test
    void doesNotInventMeasurementForMissingPricesOrUnknownPackaging() {
        assertThat(MeasurementPrice.calculate(null, BigDecimal.ONE, "L")).isNull();
        assertThat(MeasurementPrice.calculate(BigDecimal.ONE, null, "L")).isNull();
        assertThat(MeasurementPrice.calculate(BigDecimal.ONE, BigDecimal.ZERO, "G")).isNull();
        assertThat(MeasurementPrice.calculate(BigDecimal.ONE, BigDecimal.ONE, "UNKNOWN")).isNull();
    }
}
