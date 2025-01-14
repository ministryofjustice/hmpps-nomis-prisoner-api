package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateInternetAddressDsl.Companion.EMAIL
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Caseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporatePhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class CorporateDslMarker

@NomisDataDslMarker
interface CorporateDsl {
  @CorporateTypeDslMarker
  fun type(
    typeCode: String = "YOTWORKER",
    dsl: CorporateTypeDsl.() -> Unit = { },
  ): CorporateType

  @CorporateAddressDslMarker
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
    dsl: CorporateAddressDsl.() -> Unit = {},
  ): CorporateAddress

  @CorporatePhoneDslMarker
  fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: CorporatePhoneDsl.() -> Unit = {},
  ): CorporatePhone

  @CorporateInternetAddressDslMarker
  fun internetAddress(
    internetAddress: String,
    internetAddressClass: String = EMAIL,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: CorporateInternetAddressDsl.() -> Unit = {},
  ): CorporateInternetAddress
}

@Component
class CorporateBuilderFactory(
  private val repository: CorporateBuilderRepository,
  private val corporateTypeBuilderFactory: CorporateTypeBuilderFactory,
  private val corporateAddressBuilderFactory: CorporateAddressBuilderFactory,
  private val corporatePhoneBuilderFactory: CorporatePhoneBuilderFactory,
  private val corporateInternetAddressBuilderFactory: CorporateInternetAddressBuilderFactory,
) {
  fun builder(): CorporateBuilder = CorporateBuilder(repository, corporateTypeBuilderFactory, corporateAddressBuilderFactory, corporatePhoneBuilderFactory, corporateInternetAddressBuilderFactory)
}

@Component
class CorporateBuilderRepository(
  private val corporateRepository: CorporateRepository,
  private val caseloadRepository: CaseloadRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(corporate: Corporate): Corporate = corporateRepository.saveAndFlush(corporate)
  fun caseloadOf(code: String?): Caseload? = code?.let { caseloadRepository.findByIdOrNull(it) }
  fun updateCreateDatetime(corporate: Corporate, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update CORPORATES set CREATE_DATETIME = ? where CORPORATE_ID = ?", whenCreated, corporate.id)
  }
  fun updateCreateUsername(corporate: Corporate, whoCreated: String) {
    jdbcTemplate.update("update CORPORATES set CREATE_USER_ID = ? where CORPORATE_ID = ?", whoCreated, corporate.id)
  }
}

class CorporateBuilder(
  private val repository: CorporateBuilderRepository,
  private val corporateTypeBuilderFactory: CorporateTypeBuilderFactory,
  private val corporateAddressBuilderFactory: CorporateAddressBuilderFactory,
  private val corporatePhoneBuilderFactory: CorporatePhoneBuilderFactory,
  private val corporateInternetAddressBuilderFactory: CorporateInternetAddressBuilderFactory,
) : CorporateDsl {
  private lateinit var corporate: Corporate

  fun build(
    corporateName: String,
    caseloadId: String?,
    commentText: String?,
    suspended: Boolean,
    feiNumber: String?,
    active: Boolean,
    expiryDate: LocalDate?,
    taxNo: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): Corporate = Corporate(
    corporateName = corporateName,
    caseload = caseloadId?.let { repository.caseloadOf(it) },
    commentText = commentText,
    suspended = suspended,
    feiNumber = feiNumber,
    active = active,
    expiryDate = expiryDate,
    taxNo = taxNo,
  )
    .let { repository.save(it) }
    .also {
      if (whenCreated != null) {
        repository.updateCreateDatetime(it, whenCreated)
      }
      if (whoCreated != null) {
        repository.updateCreateUsername(it, whoCreated)
      }
    }
    .also { corporate = it }

  override fun type(typeCode: String, dsl: CorporateTypeDsl.() -> Unit): CorporateType = corporateTypeBuilderFactory.builder().let { builder ->
    builder.build(
      corporate = corporate,
      typeCode = typeCode,
    )
      .also { corporate.types += it }
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
    dsl: CorporateAddressDsl.() -> Unit,
  ): CorporateAddress =
    corporateAddressBuilderFactory.builder().let { builder ->
      builder.build(
        type = type,
        corporate = corporate,
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
        .also { corporate.addresses += it }
        .also { builder.apply(dsl) }
    }

  override fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: CorporatePhoneDsl.() -> Unit,
  ): CorporatePhone =
    corporatePhoneBuilderFactory.builder().let { builder ->
      builder.build(
        corporate = corporate,
        phoneType = phoneType,
        phoneNo = phoneNo,
        extNo = extNo,
        whenCreated = whenCreated,
        whoCreated = whoCreated,
      )
        .also { corporate.phones += it }
        .also { builder.apply(dsl) }
    }

  override fun internetAddress(
    internetAddress: String,
    internetAddressClass: String,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: CorporateInternetAddressDsl.() -> Unit,
  ): CorporateInternetAddress =
    corporateInternetAddressBuilderFactory.builder().let { builder ->
      builder.build(
        corporate = corporate,
        internetAddress = internetAddress,
        internetAddressClass = internetAddressClass,
        whenCreated = whenCreated,
        whoCreated = whoCreated,
      )
        .also { corporate.internetAddresses += it }
        .also { builder.apply(dsl) }
    }
}
