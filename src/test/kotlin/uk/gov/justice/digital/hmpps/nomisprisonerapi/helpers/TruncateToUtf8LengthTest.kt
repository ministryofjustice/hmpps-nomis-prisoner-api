package uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers

import com.google.common.base.Utf8
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.Test

private const val TWO_UNICODE_CHARS = "⌘⌥"

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

  @Test
  fun `truncation with unicode too long unicode at end`() {
    // ... see DPS for full text = 25 chars TWO_UNICODE_CHARS have length 6 chars
    val textTooLong = "s".repeat(17) + TWO_UNICODE_CHARS.repeat(6)
    val result = textTooLong.truncateToUtf8Length(50, true)

    assertThat(Utf8.encodedLength(result)).isEqualTo(48) // shorter than 50 because some unicode has been truncated
    assertThat(result).isEqualTo("${"s".repeat(17) + TWO_UNICODE_CHARS}... see DPS for full text")
  }
}
