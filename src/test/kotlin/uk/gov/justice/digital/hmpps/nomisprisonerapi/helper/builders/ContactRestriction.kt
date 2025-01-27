package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPersonRestrict
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RestrictionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPersonRestrictRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderPersonRestrictsDslMarker

@NomisDataDslMarker
interface OffenderPersonRestrictsDsl

@Component
class OffenderPersonRestrictsBuilderRepository(
  private val restrictionTypeRepository: ReferenceCodeRepository<RestrictionType>,
  private val jdbcTemplate: JdbcTemplate,
  private val offenderPersonRestrictRepository: OffenderPersonRestrictRepository,
) {
  fun save(restriction: OffenderPersonRestrict): OffenderPersonRestrict = offenderPersonRestrictRepository.saveAndFlush(restriction)
  fun restrictionTypeOf(code: String): RestrictionType = restrictionTypeRepository.findByIdOrNull(RestrictionType.pk(code))!!
  fun updateCreateDatetime(restriction: OffenderPersonRestrict, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update OFFENDER_PERSON_RESTRICTS set CREATE_DATETIME = ? where OFFENDER_PERSON_RESTRICT_ID = ?", whenCreated, restriction.id)
  }
  fun updateCreateUsername(restriction: OffenderPersonRestrict, whoCreated: String) {
    jdbcTemplate.update("update OFFENDER_PERSON_RESTRICTS set CREATE_USER_ID = ? where OFFENDER_PERSON_RESTRICT_ID = ?", whoCreated, restriction.id)
  }
}

@Component
class OffenderPersonRestrictsBuilderFactory(
  private val repository: OffenderPersonRestrictsBuilderRepository,
) {
  fun builder() = OffenderPersonRestrictsBuilderRepositoryBuilder(repository)
}

class OffenderPersonRestrictsBuilderRepositoryBuilder(private val repository: OffenderPersonRestrictsBuilderRepository) : OffenderPersonRestrictsDsl {
  fun build(
    contactPerson: OffenderContactPerson,
    restrictionType: String,
    enteredStaff: Staff,
    comment: String?,
    effectiveDate: LocalDate,
    expiryDate: LocalDate?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): OffenderPersonRestrict = OffenderPersonRestrict(
    contactPerson = contactPerson,
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
