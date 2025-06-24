package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderRestrictions
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RestrictionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRestrictionsRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderRestrictionsDslMarker

@NomisDataDslMarker
interface OffenderRestrictionsDsl

@Component
class OffenderRestrictionsBuilderRepository(
  private val restrictionTypeRepository: ReferenceCodeRepository<RestrictionType>,
  private val jdbcTemplate: JdbcTemplate,
  private val offenderRestrictionsRepository: OffenderRestrictionsRepository,
) {
  fun save(restriction: OffenderRestrictions): OffenderRestrictions = offenderRestrictionsRepository.saveAndFlush(restriction)
  fun restrictionTypeOf(code: String): RestrictionType = restrictionTypeRepository.findByIdOrNull(RestrictionType.pk(code))!!
  fun updateCreateDatetime(restriction: OffenderRestrictions, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update OFFENDER_RESTRICTIONS set CREATE_DATETIME = ? where OFFENDER_RESTRICTION_ID = ?", whenCreated, restriction.id)
  }
  fun updateCreateUsername(restriction: OffenderRestrictions, whoCreated: String) {
    jdbcTemplate.update("update OFFENDER_RESTRICTIONS set CREATE_USER_ID = ? where OFFENDER_RESTRICTION_ID = ?", whoCreated, restriction.id)
  }
}

@Component
class OffenderRestrictionsBuilderFactory(
  private val repository: OffenderRestrictionsBuilderRepository,
) {
  fun builder() = OffenderRestrictionsBuilderRepositoryBuilder(repository)
}

class OffenderRestrictionsBuilderRepositoryBuilder(private val repository: OffenderRestrictionsBuilderRepository) : OffenderRestrictionsDsl {
  fun build(
    offenderBooking: OffenderBooking,
    restrictionType: String,
    enteredStaff: Staff,
    authorisedStaff: Staff,
    comment: String? = null,
    effectiveDate: LocalDate,
    expiryDate: LocalDate? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
  ): OffenderRestrictions = OffenderRestrictions(
    offenderBooking = offenderBooking,
    restrictionType = repository.restrictionTypeOf(restrictionType),
    enteredStaff = enteredStaff,
    authorisedStaff = authorisedStaff,
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
