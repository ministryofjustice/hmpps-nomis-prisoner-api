package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository

@DslMarker
annotation class PersonDslMarker

@NomisDataDslMarker
interface PersonDsl

@Component
class PersonBuilderFactory(
  private val repository: PersonBuilderRepository,
) {
  fun builder(): PersonBuilder = PersonBuilder(repository)
}

@Component
class PersonBuilderRepository(
  private val personRepository: PersonRepository,
) {
  fun save(person: Person): Person = personRepository.save(person)
}

class PersonBuilder(
  private val repository: PersonBuilderRepository,
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
}
