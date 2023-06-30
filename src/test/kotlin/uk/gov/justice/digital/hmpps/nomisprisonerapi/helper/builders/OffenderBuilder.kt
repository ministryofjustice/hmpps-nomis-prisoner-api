package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.time.LocalDate
import java.time.LocalDateTime

class OffenderBuilder(
  var nomsId: String = "A5194DY",
  var lastName: String = "NTHANDA",
  var firstName: String = "LEKAN",
  var birthDate: LocalDate = LocalDate.of(1965, 7, 19),
  var genderCode: String = "M",
  var bookingBuilders: List<OffenderBookingBuilder> = mutableListOf(),
  val repository: Repository? = null,
  val courseAllocationBuilderFactory: CourseAllocationBuilderFactory? = null,
) : OffenderDsl {
  fun build(gender: Gender): Offender =
    Offender(
      nomsId = nomsId,
      lastName = lastName,
      firstName = firstName,
      birthDate = birthDate,
      gender = gender,
      lastNameKey = lastName.uppercase(),
    )

  fun withBooking(vararg bookingBuilder: OffenderBookingBuilder): OffenderBuilder {
    bookingBuilders = arrayOf(*bookingBuilder).asList()
    return this
  }

  override fun booking(
    bookingBeginDate: LocalDateTime,
    active: Boolean,
    inOutStatus: String,
    youthAdultCode: String,
    visitBalanceBuilder: VisitBalanceBuilder?,
    agencyLocationId: String,
    dsl: BookingDsl.() -> Unit,
  ) {
    this.bookingBuilders += OffenderBookingBuilder(
      bookingBeginDate = bookingBeginDate,
      active = active,
      inOutStatus = inOutStatus,
      youthAdultCode = youthAdultCode,
      visitBalanceBuilder = visitBalanceBuilder,
      agencyLocationId = agencyLocationId,
      repository = repository,
      courseAllocationBuilderFactory = courseAllocationBuilderFactory,
    ).apply(dsl)
  }
}
