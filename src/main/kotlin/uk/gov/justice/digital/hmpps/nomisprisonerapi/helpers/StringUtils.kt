package uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers

import com.google.common.base.Utf8
const val SEE_DPS = "... see DPS for full text"

fun String.truncateToUtf8Length(maxLength: Int, includeSeeDpsSuffix: Boolean = false): String {
  // ensure doesn't exceed the number of bytes Oracle can take - allowing for suffix to be added
  return if (Utf8.encodedLength(this) <= maxLength) {
    this
  } else {
    val suffix = if (includeSeeDpsSuffix) SEE_DPS else ""
    this.truncateByOneCharacterUntilFitToUtf8Length(maxLength - suffix.length) + suffix
  }
}
private fun String.truncateByOneCharacterUntilFitToUtf8Length(maxLength: Int): String {
  // so we don't cut into a double/triple byte character while truncating check
  // we still have a valid string before returning, even if it fits the number of bytes
  if (this.isStillValid() && Utf8.encodedLength(this) <= maxLength) return this
  // Keep knocking of the last character until it fits the number of bytes and remains a valid string
  return this.take(this.length - 1).truncateByOneCharacterUntilFitToUtf8Length(maxLength)
}

// Utf8.encodedLength will throw if the resulting String is cut at the incorrect boundary
private fun String.isStillValid(): Boolean = runCatching { Utf8.encodedLength(this) }.map { true }.getOrDefault(false)
