package uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers

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
  fun `will truncate when too big with huge byte characters at beginning`() {
    assertThat("😀123456789".truncateToUtf8Length(10)).isEqualTo("😀123456")
  }

  @Test
  fun `will truncate when too big with huge byte characters at end`() {
    assertThat("123456789😀".truncateToUtf8Length(10)).isEqualTo("123456789")
  }

  @Test
  fun `will truncate when dps suffix and too big with huge byte characters at beginning`() {
    assertThat("😀12345678901234567890123456789".truncateToUtf8Length(31, true)).isEqualTo("😀12$SEE_DPS")
  }

  @Test
  fun `will not truncate when text and no dps suffix needed`() {
    assertThat("1234".truncateToUtf8Length(30, true)).isEqualTo("1234")
  }

  @Test
  fun `will not truncate when text fits exactly`() {
    assertThat("12345".truncateToUtf8Length(5, true)).isEqualTo("12345")
  }

  @Test
  fun `will truncate when text fits too big`() {
    assertThat("1234567890123456789012345678901".truncateToUtf8Length(30, true)).isEqualTo("12345$SEE_DPS")
  }

  @Test
  fun `will truncate when dps suffix and too big with huge byte characters at end`() {
    assertThat("12345678901234567890123456789😀".truncateToUtf8Length(30, true)).isEqualTo("12345$SEE_DPS")
  }
}
