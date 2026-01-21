package uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers

import com.google.common.base.Utf8

const val SEE_DPS = "... see DPS for full text"

fun String.truncateToUtf8Length(maxLength: Int, includeSeeDpsSuffix: Boolean = false): String {
  // ensure doesn't exceed the number of bytes Oracle can take - allowing for suffix to be added

  if (Utf8.encodedLength(this) <= maxLength) {
    return this
  }

  var truncated: String = this
  val suffix = if (includeSeeDpsSuffix) SEE_DPS else ""
  val checkLength = maxLength - suffix.length

  // so we don't cut into a double/triple byte character while truncating check that
  // we still have a valid string before returning, even if it fits the number of bytes
  while (!truncated.isStillValid() || Utf8.encodedLength(truncated) > checkLength) {
    // Keep knocking off the last character until it fits the number of bytes and remains a valid string
    truncated = truncated.take(truncated.length - 1)
  }
  return truncated + suffix
}

// Utf8.encodedLength will throw if the resulting String is cut at the incorrect boundary
private fun String.isStillValid(): Boolean = runCatching { Utf8.encodedLength(this) }.map { true }.getOrDefault(false)
