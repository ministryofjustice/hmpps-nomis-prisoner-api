package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RestrictionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitorRestriction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitorRestrictionRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class VisitorRestrictsDslMarker

@NomisDataDslMarker
interface VisitorRestrictsDsl

@Component
class VisitorRestrictsBuilderRepository(
  private val restrictionTypeRepository: ReferenceCodeRepository<RestrictionType>,
  private val visitorRestrictionRepository: VisitorRestrictionRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun restrictionTypeOf(code: String): RestrictionType = restrictionTypeRepository.findByIdOrNull(RestrictionType.pk(code))!!
  fun save(restriction: VisitorRestriction): VisitorRestriction = visitorRestrictionRepository.saveAndFlush(restriction)
  fun updateCreateDatetime(restriction: VisitorRestriction, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update VISITOR_RESTRICTIONS set CREATE_DATETIME = ? where VISITOR_RESTRICTION_ID = ?", whenCreated, restriction.id)
  }
  fun updateCreateUsername(restriction: VisitorRestriction, whoCreated: String) {
    jdbcTemplate.update("update VISITOR_RESTRICTIONS set CREATE_USER_ID = ? where VISITOR_RESTRICTION_ID = ?", whoCreated, restriction.id)
  }
}

@Component
class VisitorRestrictsBuilderFactory(
  private val repository: VisitorRestrictsBuilderRepository,
) {
  fun builder() = VisitorRestrictsBuilderRepositoryBuilder(repository)
}

class VisitorRestrictsBuilderRepositoryBuilder(private val repository: VisitorRestrictsBuilderRepository) : VisitorRestrictsDsl {
  fun build(
    person: Person,
    restrictionType: String,
    enteredStaff: Staff,
    comment: String?,
    effectiveDate: LocalDate,
    expiryDate: LocalDate?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): VisitorRestriction = VisitorRestriction(
    person = person,
    restrictionType = repository.restrictionTypeOf(restrictionType),
    enteredStaff = enteredStaff,
    comment = comment,
    effectiveDate = effectiveDate,
    expiryDate = expiryDate,
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
