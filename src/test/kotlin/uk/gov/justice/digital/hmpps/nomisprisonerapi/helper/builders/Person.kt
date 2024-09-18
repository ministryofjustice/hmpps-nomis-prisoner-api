package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Language
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MaritalStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress
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
    dsl: PersonAddressDsl.() -> Unit = {},
  ): PersonAddress

  @PersonPhoneDslMarker
  fun phone(
    phoneType: String,
    phoneNo: String,
    extNo: String? = null,
    dsl: PersonPhoneDsl.() -> Unit = {},
  ): PersonPhone
}

@Component
class PersonBuilderFactory(
  private val repository: PersonBuilderRepository,
  private val personAddressBuilderFactory: PersonAddressBuilderFactory,
  private val personPhoneBuilderFactory: PersonPhoneBuilderFactory,
) {
  fun builder(): PersonBuilder = PersonBuilder(repository, personAddressBuilderFactory, personPhoneBuilderFactory)
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
}
