package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmployment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmploymentPK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonEmploymentRepository
import java.time.LocalDateTime

@DslMarker
annotation class PersonEmploymentDslMarker

@NomisDataDslMarker
interface PersonEmploymentDsl

@Component
class PersonEmploymentBuilderRepository(
  private val personEmploymentRepository: PersonEmploymentRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(personEmployment: PersonEmployment): PersonEmployment = personEmploymentRepository.saveAndFlush(personEmployment)
  fun updateCreateDatetime(personEmployment: PersonEmployment, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update PERSON_EMPLOYMENTS set CREATE_DATETIME = ? where PERSON_ID = ? and  EMPLOYMENT_SEQ = ?", whenCreated, personEmployment.id.person.id, personEmployment.id.sequence)
  }
  fun updateCreateUsername(personEmployment: PersonEmployment, whoCreated: String) {
    jdbcTemplate.update("update PERSON_EMPLOYMENTS set CREATE_USER_ID = ? where PERSON_ID = ? and  EMPLOYMENT_SEQ = ?", whoCreated, personEmployment.id.person.id, personEmployment.id.sequence)
  }
}

@Component
class PersonEmploymentBuilderFactory(val repository: PersonEmploymentBuilderRepository) {
  fun builder() = PersonEmploymentBuilder(repository)
}

class PersonEmploymentBuilder(val repository: PersonEmploymentBuilderRepository) : PersonEmploymentDsl {

  fun build(
    person: Person,
    sequence: Long,
    active: Boolean,
    employerCorporate: Corporate,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): PersonEmployment = PersonEmployment(
    id = PersonEmploymentPK(person, sequence),
    active = active,
    employerCorporate = employerCorporate,
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
}
