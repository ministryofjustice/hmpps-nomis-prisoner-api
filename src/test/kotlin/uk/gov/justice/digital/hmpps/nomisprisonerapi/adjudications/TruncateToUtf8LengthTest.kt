package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.Test

class TruncateToUtf8LengthTest {
  @Test
  fun `will not truncate when not even close to being too big`() {
    assertThat("123".truncateToUtf8Length(10)).isEqualTo("123")
  }

  @Test
  fun `will not truncate when just the right size`() {
    assertThat("1234567890".truncateToUtf8Length(10)).isEqualTo("1234567890")
  }

  @Test
  fun `will truncate when too big`() {
    assertThat("12345678901".truncateToUtf8Length(10)).isEqualTo("1234567890")
  }

  @Test
  fun `will truncate when too big with triple byte characters`() {
    assertThat("’123456789".truncateToUtf8Length(10)).isEqualTo("’1234567")
  }

  @Test
  fun `will truncate when too big with huge byte characters`() {
    assertThat("😀123456789".truncateToUtf8Length(10)).isEqualTo("😀123456")
  }
}
