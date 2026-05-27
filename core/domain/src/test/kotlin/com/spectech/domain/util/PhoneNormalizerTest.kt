package com.spectech.domain.util

import com.spectech.domain.error.ApiError
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PhoneNormalizerTest {

    @Test fun `10 subscriber digits get +7 prefix`() {
        PhoneNormalizer.normalizeRussian("9991234567") shouldBe "+79991234567"
    }

    @Test fun `11 digits starting with 8 are converted to +7`() {
        PhoneNormalizer.normalizeRussian("89991234567") shouldBe "+79991234567"
    }

    @Test fun `11 digits starting with 7 get a + prepended`() {
        PhoneNormalizer.normalizeRussian("79991234567") shouldBe "+79991234567"
    }

    @Test fun `+7 form is preserved end-to-end`() {
        PhoneNormalizer.normalizeRussian("+79991234567") shouldBe "+79991234567"
    }

    @Test fun `formatted display strings are normalised`() {
        PhoneNormalizer.normalizeRussian("+7 (999) 123-45-67") shouldBe "+79991234567"
        PhoneNormalizer.normalizeRussian("8 (999) 123 45 67") shouldBe "+79991234567"
    }

    @Test fun `empty input throws InvalidPhone`() {
        assertThrows<ApiError> { PhoneNormalizer.normalizeRussian("") }
    }

    @Test fun `too few digits throws InvalidPhone`() {
        assertThrows<ApiError> { PhoneNormalizer.normalizeRussian("123") }
    }

    @Test fun `too many digits throws InvalidPhone`() {
        assertThrows<ApiError> { PhoneNormalizer.normalizeRussian("99912345678901") }
    }
}
