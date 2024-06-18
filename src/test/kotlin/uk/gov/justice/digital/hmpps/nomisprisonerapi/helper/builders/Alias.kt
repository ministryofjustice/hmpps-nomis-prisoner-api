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
annotation class AliasDslMarker

@NomisDataDslMarker
interface AliasDsl {
  @BookingDslMarker
  fun booking(
    bookingBeginDate: LocalDateTime = LocalDateTime.now(),
    active: Boolean = true,
    inOutStatus: String = "IN",
    youthAdultCode: String = "N",
    agencyLocationId: String = "BXI",
    livingUnitId: Long = -3009,
    bookingSequence: Int? = null,
    dsl: BookingDsl.() -> Unit = {},
  ): OffenderBooking
}

@Component
class AliasBuilderRepository(
  private val offenderRepository: OffenderRepository,
  private val genderRepository: ReferenceCodeRepository<Gender>,
) {
  fun save(offender: Offender): Offender = offenderRepository.save(offender)
  fun gender(genderCode: String) = genderRepository.findByIdOrNull(ReferenceCode.Pk(Gender.SEX, genderCode))!!
}

@Component
class AliasBuilderFactory(
  private val repository: AliasBuilderRepository,
  private val bookingBuilderFactory: BookingBuilderFactory,
) {
  fun builder(offenderBuilder: OffenderBuilder): AliasBuilder =
    AliasBuilder(repository, bookingBuilderFactory, offenderBuilder)
}

class AliasBuilder(
  private val repository: AliasBuilderRepository,
  private val bookingBuilderFactory: BookingBuilderFactory,
  private val offenderBuilder: OffenderBuilder,
) : AliasDsl {
  private lateinit var aliasOffender: Offender

  fun build(
    lastName: String,
    firstName: String,
    birthDate: LocalDate,
    genderCode: String,
  ): Offender = Offender(
    nomsId = offenderBuilder.rootOffender.nomsId,
    lastName = lastName,
    firstName = firstName,
    birthDate = birthDate,
    gender = repository.gender(genderCode),
    lastNameKey = lastName.uppercase(),
    rootOffenderId = offenderBuilder.rootOffender.rootOffenderId,
    rootOffender = offenderBuilder.rootOffender.rootOffender,
  )
    .let { repository.save(it) }
    .also {
      aliasOffender = it
    }

  override fun booking(
    bookingBeginDate: LocalDateTime,
    active: Boolean,
    inOutStatus: String,
    youthAdultCode: String,
    agencyLocationId: String,
    livingUnitId: Long,
    bookingSequence: Int?,
    dsl: BookingDsl.() -> Unit,
  ) = bookingBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offender = aliasOffender,
        // if you want multiple bookings then create them with latest booking first so it gets seq 1 like in Nomis,
        // or override the sequences
        bookingSequence = bookingSequence ?: offenderBuilder.nextBookingSequence++,
        agencyLocationCode = agencyLocationId,
        bookingBeginDate = bookingBeginDate,
        active = active,
        inOutStatus = inOutStatus,
        youthAdultCode = youthAdultCode,
        livingUnitId = livingUnitId,
      )
        .also {
          aliasOffender.bookings += it
          offenderBuilder.rootOffender.getAllBookings()?.add(it)
        }
        .also { builder.apply(dsl) }
    }
}
