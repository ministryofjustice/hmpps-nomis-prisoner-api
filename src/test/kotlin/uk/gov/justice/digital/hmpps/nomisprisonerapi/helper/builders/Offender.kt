package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Country
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Ethnicity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NameType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTrustAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Title
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.plusAssign

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
    titleCode: String? = null,
    lastName: String = "NTHANDA",
    firstName: String = "LEKAN",
    middleName: String? = null,
    middleName2: String? = null,
    birthDate: LocalDate = LocalDate.of(1965, 7, 19),
    ethnicityCode: String? = null,
    genderCode: String = "M",
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: AliasDsl.() -> Unit = {},
  ): Offender

  @OffenderIdentifierDslMarker
  fun identifier(
    type: String = "NINO",
    identifier: String = "NE112233T",
    issuedAuthority: String? = null,
    issuedDate: LocalDate? = null,
    verified: Boolean? = null,
    dsl: OffenderIdentifierDsl.() -> Unit = {},
  ): OffenderIdentifier

  @OffenderAddressDslMarker
  fun address(
    type: String? = null,
    premise: String? = "41",
    street: String? = "High Street",
    locality: String? = "Sheffield",
    flat: String? = null,
    postcode: String? = null,
    city: String? = null,
    county: String? = null,
    country: String? = null,
    validatedPAF: Boolean = false,
    noFixedAddress: Boolean? = null,
    primaryAddress: Boolean = false,
    mailAddress: Boolean = false,
    comment: String? = null,
    startDate: String? = null,
    endDate: String? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: OffenderAddressDsl.() -> Unit = {},
  ): OffenderAddress

  @OffenderPhoneDslMarker
  fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: OffenderPhoneDsl.() -> Unit = {},
  ): OffenderPhone

  @OffenderEmailDslMarker
  fun email(
    emailAddress: String,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: OffenderEmailDsl.() -> Unit = {},
  ): OffenderInternetAddress

  @OffenderTrustAccountDslMarker
  fun trustAccount(
    caseloadId: String = "MDI",
    currentBalance: BigDecimal = BigDecimal.ZERO,
    holdBalance: BigDecimal = BigDecimal.ZERO,
    dsl: OffenderTrustAccountDsl.() -> Unit = {},
  ): OffenderTrustAccount
}

@Component
class OffenderBuilderRepository(
  private val offenderRepository: OffenderRepository,
  private val ethnicityRepository: ReferenceCodeRepository<Ethnicity>,
  private val genderRepository: ReferenceCodeRepository<Gender>,
  private val titleRepository: ReferenceCodeRepository<Title>,
  private val countryRepository: ReferenceCodeRepository<Country>,
  private val nameTypeRepository: ReferenceCodeRepository<NameType>,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(offender: Offender): Offender = offenderRepository.saveAndFlush(offender)
  fun ethnicity(ethnicityCode: String): Ethnicity = ethnicityRepository.findByIdOrNull(ReferenceCode.Pk(Ethnicity.ETHNICITY, ethnicityCode))!!
  fun gender(genderCode: String): Gender = genderRepository.findByIdOrNull(ReferenceCode.Pk(Gender.SEX, genderCode))!!
  fun title(titleCode: String): Title = titleRepository.findByIdOrNull(ReferenceCode.Pk(Title.TITLE, titleCode))!!
  fun country(birthCountryCode: String): Country = countryRepository.findByIdOrNull(ReferenceCode.Pk(Country.COUNTRY, birthCountryCode))!!
  fun nameType(nameTypeCode: String): NameType = nameTypeRepository.findByIdOrNull(ReferenceCode.Pk(NameType.NAME_TYPE, nameTypeCode))!!
  fun updateCreateDatetime(offender: Offender, whenCreated: LocalDateTime) = jdbcTemplate.update("update OFFENDERS set CREATE_DATETIME = ? where OFFENDER_ID = ?", whenCreated, offender.id)
  fun updateCreateUsername(offender: Offender, whoCreated: String) = jdbcTemplate.update("update OFFENDERS set CREATE_USER_ID = ? where OFFENDER_ID = ?", whoCreated, offender.id)
}

@Component
class OffenderBuilderFactory(
  private val repository: OffenderBuilderRepository,
  private val bookingBuilderFactory: BookingBuilderFactory,
  private val aliasBuilderFactory: AliasBuilderFactory,
  private val offenderIdentifierBuilderFactory: OffenderIdentifierBuilderFactory,
  private val offenderAddressBuilderFactory: OffenderAddressBuilderFactory,
  private val offenderPhoneBuilderFactory: OffenderPhoneBuilderFactory,
  private val offenderEmailBuilderFactory: OffenderEmailBuilderFactory,
  private val offenderTrustAccountBuilderFactory: OffenderTrustAccountBuilderFactory,
) {
  fun builder(): OffenderBuilder = OffenderBuilder(
    repository,
    bookingBuilderFactory,
    aliasBuilderFactory,
    offenderIdentifierBuilderFactory,
    offenderAddressBuilderFactory,
    offenderPhoneBuilderFactory,
    offenderEmailBuilderFactory,
    offenderTrustAccountBuilderFactory,
  )
}

class OffenderBuilder(
  private val repository: OffenderBuilderRepository,
  private val bookingBuilderFactory: BookingBuilderFactory,
  private val aliasBuilderFactory: AliasBuilderFactory,
  private val offenderIdentifierBuilderFactory: OffenderIdentifierBuilderFactory,
  private val offenderAddressBuilderFactory: OffenderAddressBuilderFactory,
  private val offenderPhoneBuilderFactory: OffenderPhoneBuilderFactory,
  private val offenderEmailBuilderFactory: OffenderEmailBuilderFactory,
  private val offenderTrustAccountBuilderFactory: OffenderTrustAccountBuilderFactory,
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
    birthCountryCode: String?,
    ethnicityCode: String?,
    genderCode: String,
    nameTypeCode: String?,
    createDate: LocalDate,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): Offender = Offender(
    nomsId = nomsId,
    title = titleCode?.let { repository.title(titleCode) },
    lastName = lastName,
    firstName = firstName,
    middleName = middleName,
    middleName2 = middleName2,
    birthDate = birthDate,
    birthPlace = birthPlace,
    birthCountry = birthCountryCode?.let { repository.country(birthCountryCode) },
    ethnicity = ethnicityCode?.let { repository.ethnicity(ethnicityCode) },
    gender = repository.gender(genderCode),
    nameType = nameTypeCode?.let { repository.nameType(nameTypeCode) },
    createDate = createDate,
    lastNameKey = lastName.uppercase(),
  )
    .let { repository.save(it) }
    .also {
      it.rootOffenderId = it.id
      it.rootOffender = it
      rootOffender = it
      whenCreated?.apply { repository.updateCreateDatetime(it, whenCreated) }
      whoCreated?.apply { repository.updateCreateUsername(it, whoCreated) }
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
    titleCode: String?,
    lastName: String,
    firstName: String,
    middleName: String?,
    middleName2: String?,
    birthDate: LocalDate,
    ethnicityCode: String?,
    genderCode: String,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: AliasDsl.() -> Unit,
  ): Offender = aliasBuilderFactory.builder(this)
    .let { builder ->
      builder.build(
        titleCode = titleCode,
        lastName = lastName,
        firstName = firstName,
        middleName = middleName,
        middleName2 = middleName2,
        birthDate = birthDate,
        ethnicityCode = ethnicityCode,
        genderCode = genderCode,
        whenCreated = whenCreated,
        whoCreated = whoCreated,
      )
        .also { builder.apply(dsl) }
    }

  override fun identifier(
    type: String,
    identifier: String,
    issuedAuthority: String?,
    issuedDate: LocalDate?,
    verified: Boolean?,
    dsl: OffenderIdentifierDsl.() -> Unit,
  ): OffenderIdentifier = offenderIdentifierBuilderFactory.builder().let { builder ->
    builder.build(
      offender = rootOffender,
      sequence = rootOffender.identifiers.size + 1L,
      type = type,
      identifier = identifier,
      issuedAuthority = issuedAuthority,
      issuedDate = issuedDate,
      verified = verified,
    )
      .also { rootOffender.identifiers += it }
      .also { builder.apply(dsl) }
  }

  override fun address(
    type: String?,
    premise: String?,
    street: String?,
    locality: String?,
    flat: String?,
    postcode: String?,
    city: String?,
    county: String?,
    country: String?,
    validatedPAF: Boolean,
    noFixedAddress: Boolean?,
    primaryAddress: Boolean,
    mailAddress: Boolean,
    comment: String?,
    startDate: String?,
    endDate: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: OffenderAddressDsl.() -> Unit,
  ): OffenderAddress = offenderAddressBuilderFactory.builder().let { builder ->
    builder.build(
      type = type,
      offender = rootOffender,
      premise = premise,
      street = street,
      locality = locality,
      flat = flat,
      postcode = postcode,
      city = city,
      county = county,
      country = country,
      validatedPAF = validatedPAF,
      noFixedAddress = noFixedAddress,
      primaryAddress = primaryAddress,
      mailAddress = mailAddress,
      comment = comment,
      startDate = startDate?.let { LocalDate.parse(it) },
      endDate = endDate?.let { LocalDate.parse(it) },
      whoCreated = whoCreated,
      whenCreated = whenCreated,
    )
      .also { rootOffender.addresses += it }
      .also { builder.apply(dsl) }
  }

  override fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: OffenderPhoneDsl.() -> Unit,
  ): OffenderPhone = offenderPhoneBuilderFactory.builder().let { builder ->
    builder.build(
      offender = rootOffender,
      phoneType = phoneType,
      phoneNo = phoneNo,
      extNo = extNo,
      whenCreated = whenCreated,
      whoCreated = whoCreated,
    )
      .also { rootOffender.phones += it }
      .also { builder.apply(dsl) }
  }

  override fun email(
    emailAddress: String,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: OffenderEmailDsl.() -> Unit,
  ): OffenderInternetAddress = offenderEmailBuilderFactory.builder().let { builder ->
    builder.build(
      offender = rootOffender,
      emailAddress = emailAddress,
      whenCreated = whenCreated,
      whoCreated = whoCreated,
    )
      .also { rootOffender.internetAddresses += it }
      .also { builder.apply(dsl) }
  }

  override fun trustAccount(
    caseloadId: String,
    currentBalance: BigDecimal,
    holdBalance: BigDecimal,
    dsl: OffenderTrustAccountDsl.() -> Unit,
  ): OffenderTrustAccount = offenderTrustAccountBuilderFactory.builder().let { builder ->
    builder.build(
      offender = rootOffender,
      caseloadId = caseloadId,
      currentBalance = currentBalance,
      holdBalance = holdBalance,
    ).also { rootOffender.trustAccounts += it }
      .also { builder.apply(dsl) }
  }
}
