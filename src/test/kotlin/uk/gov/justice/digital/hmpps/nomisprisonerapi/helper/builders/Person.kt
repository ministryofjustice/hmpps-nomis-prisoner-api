package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository

@DslMarker
annotation class PersonDslMarker

@NomisDataDslMarker
interface PersonDsl {

  @PersonAddressDslMarker
  fun address(
    premise: String = "41",
    street: String = "High Street",
    locality: String = "Sheffield",
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
) {
  fun save(person: Person): Person = personRepository.save(person)
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
  ): Person = Person(
    lastName = lastName,
    firstName = firstName,
  )
    .let { repository.save(it) }
    .also { person = it }

  override fun address(
    premise: String,
    street: String,
    locality: String,
    dsl: PersonAddressDsl.() -> Unit,
  ): PersonAddress =
    personAddressBuilderFactory.builder().let { builder ->
      builder.build(
        person = person,
        premise = premise,
        street = street,
        locality = locality,
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
