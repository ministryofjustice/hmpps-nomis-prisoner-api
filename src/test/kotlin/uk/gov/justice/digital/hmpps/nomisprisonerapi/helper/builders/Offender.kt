package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderDslMarker

@DslMarker
annotation class AliasDslMarker

@NomisDataDslMarker
interface OffenderDsl {
  @BookingDslMarker
  fun booking(
    bookingBeginDate: LocalDateTime = LocalDateTime.now(),
    active: Boolean = true,
    inOutStatus: String = "IN",
    youthAdultCode: String = "N",
    agencyLocationId: String = "BXI",
    livingUnitId: Long = -3009,
    dsl: BookingDsl.() -> Unit = {},
  ): OffenderBooking

  @AliasDslMarker
  fun alias(
    lastName: String = "NTHANDA",
    firstName: String = "LEKAN",
    birthDate: LocalDate = LocalDate.of(1965, 7, 19),
    genderCode: String = "M",
    dsl: OffenderDsl.() -> Unit = {},
  ): Offender
}

@Component
class OffenderBuilderRepository(
  private val offenderRepository: OffenderRepository,
  private val genderRepository: ReferenceCodeRepository<Gender>,
) {
  fun save(offender: Offender): Offender = offenderRepository.save(offender)
  fun gender(genderCode: String) = genderRepository.findByIdOrNull(ReferenceCode.Pk(Gender.SEX, genderCode))!!
}

@Component
class OffenderBuilderFactory(
  private val repository: OffenderBuilderRepository,
  private val bookingBuilderFactory: BookingBuilderFactory,
) {
  fun builder(): OffenderBuilder {
    return OffenderBuilder(repository, bookingBuilderFactory)
  }
}

class OffenderBuilder(
  private val repository: OffenderBuilderRepository,
  private val bookingBuilderFactory: BookingBuilderFactory,
) : OffenderDsl {
  private lateinit var offender: Offender

  fun build(
    nomsId: String,
    lastName: String,
    firstName: String,
    birthDate: LocalDate,
    genderCode: String,
  ): Offender = Offender(
    nomsId = nomsId,
    lastName = lastName,
    firstName = firstName,
    birthDate = birthDate,
    gender = repository.gender(genderCode),
    lastNameKey = lastName.uppercase(),
  )
    .let { repository.save(it) }
    .also { it.rootOffenderId = it.id }
    .also { offender = it }

  private fun buildAlias(
    lastName: String,
    firstName: String,
    birthDate: LocalDate,
    genderCode: String,
  ): Offender = Offender(
    nomsId = offender.nomsId,
    lastName = lastName,
    firstName = firstName,
    birthDate = birthDate,
    gender = repository.gender(genderCode),
    lastNameKey = lastName.uppercase(),
    rootOffenderId = offender.rootOffenderId,
  )
    .let { repository.save(it) }

  override fun booking(
    bookingBeginDate: LocalDateTime,
    active: Boolean,
    inOutStatus: String,
    youthAdultCode: String,
    agencyLocationId: String,
    livingUnitId: Long,
    dsl: BookingDsl.() -> Unit,
  ) = bookingBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offender = offender,
        // if you want multiple bookings then create them with latest booking first so it gets seq 1 like in Nomis
        bookingSequence = offender.bookings.size + 1,
        agencyLocationCode = agencyLocationId,
        bookingBeginDate = bookingBeginDate,
        active = active,
        inOutStatus = inOutStatus,
        youthAdultCode = youthAdultCode,
        livingUnitId = livingUnitId,
      )
        .also { offender.bookings += it }
        .also { builder.apply(dsl) }
    }

  override fun alias(
    lastName: String,
    firstName: String,
    birthDate: LocalDate,
    genderCode: String,
    dsl: OffenderDsl.() -> Unit,
  ): Offender =
    buildAlias(lastName, firstName, birthDate, genderCode)
      .also { apply(dsl) }
}
