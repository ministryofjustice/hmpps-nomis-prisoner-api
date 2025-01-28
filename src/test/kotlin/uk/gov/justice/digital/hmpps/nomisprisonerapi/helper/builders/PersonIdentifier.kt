package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IdentifierType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonIdentifierPK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonIdentifierRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@DslMarker
annotation class PersonIdentifierDslMarker

@NomisDataDslMarker
interface PersonIdentifierDsl

@Component
class PersonIdentifierBuilderRepository(
  private val personIdentifierRepository: PersonIdentifierRepository,
  private val jdbcTemplate: JdbcTemplate,
  private val identifierTypeRepository: ReferenceCodeRepository<IdentifierType>,
) {
  fun identifierTypeOf(code: String): IdentifierType = identifierTypeRepository.findByIdOrNull(IdentifierType.pk(code))!!
  fun save(personIdentifier: PersonIdentifier): PersonIdentifier = personIdentifierRepository.saveAndFlush(personIdentifier)
  fun updateCreateDatetime(personIdentifier: PersonIdentifier, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update PERSON_IDENTIFIERS set CREATE_DATETIME = ? where PERSON_ID = ? and  ID_SEQ = ?", whenCreated, personIdentifier.id.person.id, personIdentifier.id.sequence)
  }
  fun updateCreateUsername(personIdentifier: PersonIdentifier, whoCreated: String) {
    jdbcTemplate.update("update PERSON_IDENTIFIERS set CREATE_USER_ID = ? where PERSON_ID = ? and  ID_SEQ = ?", whoCreated, personIdentifier.id.person.id, personIdentifier.id.sequence)
  }
}

@Component
class PersonIdentifierBuilderFactory(val repository: PersonIdentifierBuilderRepository) {
  fun builder() = PersonIdentifierBuilder(repository)
}

class PersonIdentifierBuilder(
  private val repository: PersonIdentifierBuilderRepository,
) : PersonIdentifierDsl {

  private lateinit var personIdentifier: PersonIdentifier

  fun build(
    person: Person,
    sequence: Long,
    type: String,
    identifier: String,
    issuedAuthority: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): PersonIdentifier = PersonIdentifier(
    id = PersonIdentifierPK(person, sequence),
    identifierType = repository.identifierTypeOf(type),
    identifier = identifier,
    issuedAuthority = issuedAuthority,
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
    .also { personIdentifier = it }
}
