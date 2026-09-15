package br.com.supermercados.prices.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.supermercados.prices.common.ApiException;

class GtinTest {

    @ParameterizedTest
    @CsvSource({
            "12345670, 00000012345670",
            "012345678905, 00012345678905",
            "0000000000017, 00000000000017",
            "00000000000017, 00000000000017"
    })
    void acceptsSupportedFormatsAndPreservesIdentity(String input, String expected) {
        assertThat(Gtin.normalize(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345671", "0000000000018", "12345", "abcdefgh", "1234-5670", "１２３４５６７０"})
    void rejectsInvalidFormatsAndCheckDigits(String input) {
        assertThatThrownBy(() -> Gtin.normalize(input)).isInstanceOf(ApiException.class);
    }

    @Test
    void missingGtinRemainsUnknown() {
        assertThat(Gtin.normalize(null)).isNull();
        assertThat(Gtin.normalize(" ")).isNull();
    }
}
