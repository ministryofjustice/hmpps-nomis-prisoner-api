package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Ethnicity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Title
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderDslMarker

@NomisDataDslMarker
interface OffenderDsl {
  @BookingDslMarker
  fun booking(
    bookingBeginDate: LocalDateTime = LocalDateTime.now(),
    // This is a date because NOMIS uses a DATE column which truncates the time
    bookingEndDate: LocalDate? = null,
    active: Boolean = true,
    inOutStatus: String = "IN",
    youthAdultCode: String = "N",
    agencyLocationId: String = "BXI",
    livingUnitId: Long = -3009,
    bookingSequence: Int? = null,
    dsl: BookingDsl.() -> Unit = {},
  ): OffenderBooking

  @AliasDslMarker
  fun alias(
    lastName: String = "NTHANDA",
    firstName: String = "LEKAN",
    birthDate: LocalDate = LocalDate.of(1965, 7, 19),
    genderCode: String = "M",
    dsl: AliasDsl.() -> Unit = {},
  ): Offender
}

@Component
class OffenderBuilderRepository(
  private val offenderRepository: OffenderRepository,
  private val ethnicityRepository: ReferenceCodeRepository<Ethnicity>,
  private val genderRepository: ReferenceCodeRepository<Gender>,
  private val titleRepository: ReferenceCodeRepository<Title>,
) {
  fun save(offender: Offender): Offender = offenderRepository.save(offender)
  fun ethnicity(ethnicityCode: String): Ethnicity = ethnicityRepository.findByIdOrNull(ReferenceCode.Pk(Ethnicity.ETHNICITY, ethnicityCode))!!
  fun gender(genderCode: String): Gender = genderRepository.findByIdOrNull(ReferenceCode.Pk(Gender.SEX, genderCode))!!
  fun title(titleCode: String): Title = titleRepository.findByIdOrNull(ReferenceCode.Pk(Title.TITLE, titleCode))!!
}

@Component
class OffenderBuilderFactory(
  private val repository: OffenderBuilderRepository,
  private val bookingBuilderFactory: BookingBuilderFactory,
  private val aliasBuilderFactory: AliasBuilderFactory,
) {
  fun builder(): OffenderBuilder = OffenderBuilder(repository, bookingBuilderFactory, aliasBuilderFactory)
}

class OffenderBuilder(
  private val repository: OffenderBuilderRepository,
  private val bookingBuilderFactory: BookingBuilderFactory,
  private val aliasBuilderFactory: AliasBuilderFactory,
) : OffenderDsl {
  lateinit var rootOffender: Offender
  var nextBookingSequence: Int = 1

  fun build(
    nomsId: String,
    titleCode: String?,
    lastName: String,
    firstName: String,
    middleName: String?,
    middleName2: String?,
    birthDate: LocalDate?,
    birthPlace: String?,
    ethnicityCode: String?,
    genderCode: String,
  ): Offender = Offender(
    nomsId = nomsId,
    title = titleCode?.let { repository.title(it) },
    lastName = lastName,
    firstName = firstName,
    middleName = middleName,
    middleName2 = middleName2,
    birthDate = birthDate,
    birthPlace = birthPlace,
    ethnicity = ethnicityCode?.let { repository.ethnicity(ethnicityCode) },
    gender = repository.gender(genderCode),
    lastNameKey = lastName.uppercase(),
  )
    .let { repository.save(it) }
    .also {
      it.rootOffenderId = it.id
      it.rootOffender = it
      rootOffender = it
    }

  override fun booking(
    bookingBeginDate: LocalDateTime,
    bookingEndDate: LocalDate?,
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
        offender = rootOffender,
        // if you want multiple bookings then create them with latest booking first so it gets seq 1 like in Nomis,
        // or override the sequences
        bookingSequence = bookingSequence ?: nextBookingSequence++,
        agencyLocationCode = agencyLocationId,
        bookingBeginDate = bookingBeginDate,
        bookingEndDate = bookingEndDate,
        active = active,
        inOutStatus = inOutStatus,
        youthAdultCode = youthAdultCode,
        livingUnitId = livingUnitId,
      )
        .also {
          rootOffender.getAllBookings()?.add(it)
        }
        .also { builder.apply(dsl) }
    }

  override fun alias(
    lastName: String,
    firstName: String,
    birthDate: LocalDate,
    genderCode: String,
    dsl: AliasDsl.() -> Unit,
  ): Offender = aliasBuilderFactory.builder(this)
    .let { builder ->
      builder.build(lastName, firstName, birthDate, genderCode)
        .also { builder.apply(dsl) }
    }
}
