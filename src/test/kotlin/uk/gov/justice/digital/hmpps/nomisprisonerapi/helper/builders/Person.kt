package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Language
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MaritalStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmployment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Title
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class PersonDslMarker

@NomisDataDslMarker
interface PersonDsl {

  @PersonAddressDslMarker
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
    dsl: PersonAddressDsl.() -> Unit = {},
  ): PersonAddress

  @PersonPhoneDslMarker
  fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String? = null,
    dsl: PersonPhoneDsl.() -> Unit = {},
  ): PersonPhone

  @PersonEmailDslMarker
  fun email(
    emailAddress: String,
    dsl: PersonEmailDsl.() -> Unit = {},
  ): PersonInternetAddress

  @PersonEmploymentDslMarker
  fun employment(
    employerCorporate: Corporate? = null,
    active: Boolean = true,
    dsl: PersonEmploymentDsl.() -> Unit = {},
  ): PersonEmployment
}

@Component
class PersonBuilderFactory(
  private val repository: PersonBuilderRepository,
  private val personAddressBuilderFactory: PersonAddressBuilderFactory,
  private val personPhoneBuilderFactory: PersonPhoneBuilderFactory,
  private val personEmailBuilderFactory: PersonEmailBuilderFactory,
  private val personEmploymentBuilderFactory: PersonEmploymentBuilderFactory,
) {
  fun builder(): PersonBuilder = PersonBuilder(
    repository,
    personAddressBuilderFactory,
    personPhoneBuilderFactory,
    personEmailBuilderFactory,
    personEmploymentBuilderFactory,
  )
}

@Component
class PersonBuilderRepository(
  private val personRepository: PersonRepository,
  private val genderRepository: ReferenceCodeRepository<Gender>,
  private val titleRepository: ReferenceCodeRepository<Title>,
  private val languageRepository: ReferenceCodeRepository<Language>,
  private val maritalStatusRepository: ReferenceCodeRepository<MaritalStatus>,

) {
  fun save(person: Person): Person = personRepository.save(person)
  fun genderOf(code: String?): Gender? = code?.let { genderRepository.findByIdOrNull(Gender.pk(it)) }
  fun titleOf(code: String?): Title? = code?.let { titleRepository.findByIdOrNull(Title.pk(it)) }
  fun languageOf(code: String?): Language? = code?.let { languageRepository.findByIdOrNull(Language.pk(it)) }
  fun martialStatusOf(code: String?): MaritalStatus? = code?.let { maritalStatusRepository.findByIdOrNull(MaritalStatus.pk(it)) }
}

class PersonBuilder(
  private val repository: PersonBuilderRepository,
  private val personAddressBuilderFactory: PersonAddressBuilderFactory,
  private val personPhoneBuilderFactory: PersonPhoneBuilderFactory,
  private val personEmailBuilderFactory: PersonEmailBuilderFactory,
  private val personEmploymentBuilderFactory: PersonEmploymentBuilderFactory,
) : PersonDsl {
  private lateinit var person: Person

  fun build(
    lastName: String,
    firstName: String,
    middleName: String?,
    dateOfBirth: LocalDate?,
    gender: String?,
    title: String?,
    language: String?,
    interpreterRequired: Boolean,
    domesticStatus: String?,
    deceasedDate: LocalDate?,
    isStaff: Boolean?,
    isRemitter: Boolean?,
    keepBiometrics: Boolean,
  ): Person = Person(
    lastName = lastName,
    firstName = firstName,
    middleName = middleName,
    birthDate = dateOfBirth,
    sex = repository.genderOf(gender),
    title = repository.titleOf(title),
    language = repository.languageOf(language),
    interpreterRequired = interpreterRequired,
    domesticStatus = repository.martialStatusOf(domesticStatus),
    deceasedDate = deceasedDate,
    isStaff = isStaff,
    isRemitter = isRemitter,
    keepBiometrics = keepBiometrics,
  )
    .let { repository.save(it) }
    .also { person = it }

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
    dsl: PersonAddressDsl.() -> Unit,
  ): PersonAddress =
    personAddressBuilderFactory.builder().let { builder ->
      builder.build(
        type = type,
        person = person,
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
      )
        .also { person.addresses += it }
        .also { builder.apply(dsl) }
    }

  override fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String?,
    dsl: PersonPhoneDsl.() -> Unit,
  ): PersonPhone =
    personPhoneBuilderFactory.builder().let { builder ->
      builder.build(
        person = person,
        phoneType = phoneType,
        phoneNo = phoneNo,
        extNo = extNo,
      )
        .also { person.phones += it }
        .also { builder.apply(dsl) }
    }

  override fun email(emailAddress: String, dsl: PersonEmailDsl.() -> Unit): PersonInternetAddress =
    personEmailBuilderFactory.builder().let { builder ->
      builder.build(
        person = person,
        emailAddress = emailAddress,
      )
        .also { person.internetAddresses += it }
        .also { builder.apply(dsl) }
    }

  override fun employment(
    employerCorporate: Corporate?,
    active: Boolean,
    dsl: PersonEmploymentDsl.() -> Unit,
  ): PersonEmployment =
    personEmploymentBuilderFactory.builder().let { builder ->
      builder.build(
        person = person,
        sequence = person.employments.size + 1L,
        employerCorporate = employerCorporate,
        active = active,
      )
        .also { person.employments += it }
        .also { builder.apply(dsl) }
    }
}
