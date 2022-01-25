package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.time.LocalDate


class OffenderBuilder(
  var nomsId: String = "A5194DY",
  var lastName: String = "NTHANDA",
  var firstName: String = "LEKAN",
  var birthDate: LocalDate = LocalDate.of(1965, 7, 19),
  var genderCode: String = "M",
  var bookingBuilders: Array<OffenderBookingBuilder> = Array(1) { OffenderBookingBuilder() }
) {
  fun build(gender: Gender): Offender =
    Offender(
      nomsId = nomsId,
      lastName = lastName,
      firstName = firstName,
      birthDate = birthDate,
      gender = gender,
      lastNameKey = lastName.uppercase()
    )

  fun withBooking(vararg bookingBuilder: OffenderBookingBuilder): OffenderBuilder {
    bookingBuilders = arrayOf(*bookingBuilder)
    return this
  }

}

