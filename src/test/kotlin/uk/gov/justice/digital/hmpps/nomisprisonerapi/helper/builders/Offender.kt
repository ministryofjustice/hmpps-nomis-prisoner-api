package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class NewOffenderDslMarker

@NomisDataDslMarker
interface NewOffenderDsl : NewBookingDslApi

interface NewOffenderDslApi {
  // This allows us to slowly migrate from the old DSL to the new one. Once migration is complete we can delete @OffenderDslMarker then rename @NewOffenderDslMarker
  @NewOffenderDslMarker
  fun newOffender(
    nomsId: String = "A5194DY",
    lastName: String = "NTHANDA",
    firstName: String = "LEKAN",
    birthDate: LocalDate = LocalDate.of(1965, 7, 19),
    genderCode: String = "M",
    dsl: NewOffenderDsl.() -> Unit = {},
  ): Offender
}

@Component
class NewOffenderBuilderRepository(
  private val offenderRepository: OffenderRepository,
  private val genderRepository: ReferenceCodeRepository<Gender>,
) {
  fun save(offender: Offender) = offenderRepository.save(offender)
  fun gender(genderCode: String) = genderRepository.findByIdOrNull(ReferenceCode.Pk(Gender.SEX, genderCode))!!
}

@Component
class NewOffenderBuilderFactory(
  private val repository: NewOffenderBuilderRepository,
  private val bookingBuilderFactory: NewBookingBuilderFactory,
) {
  fun builder(): NewOffenderBuilder {
    return NewOffenderBuilder(repository, bookingBuilderFactory)
  }
}

class NewOffenderBuilder(
  private val repository: NewOffenderBuilderRepository,
  private val bookingBuilderFactory: NewBookingBuilderFactory,
) : NewOffenderDsl {
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
    .also { offender = it }

  override fun booking(
    bookingBeginDate: LocalDateTime,
    active: Boolean,
    inOutStatus: String,
    youthAdultCode: String,
    agencyLocationId: String,
    dsl: NewBookingDsl.() -> Unit,
  ) = bookingBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offender = offender,
        bookingSequence = offender.bookings.size + 1, // if you want multiple bookings then create them with latest booking first so it gets seq 1 like in Nomis
        agencyLocationCode = agencyLocationId,
        bookingBeginDate = bookingBeginDate,
        active = active,
        inOutStatus = inOutStatus,
        youthAdultCode = youthAdultCode,
      )
        .also { offender.bookings += it }
        .also { builder.apply(dsl) }
    }
}
