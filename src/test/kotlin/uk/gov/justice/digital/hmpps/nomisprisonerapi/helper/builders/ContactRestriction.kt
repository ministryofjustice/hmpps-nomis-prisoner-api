package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPersonRestrict
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RestrictionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class OffenderPersonRestrictsDslMarker

@NomisDataDslMarker
interface OffenderPersonRestrictsDsl

@Component
class OffenderPersonRestrictsBuilderRepository(
  private val restrictionTypeRepository: ReferenceCodeRepository<RestrictionType>,
) {
  fun restrictionTypeOf(code: String): RestrictionType = restrictionTypeRepository.findByIdOrNull(RestrictionType.pk(code))!!
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
  ): OffenderPersonRestrict =
    OffenderPersonRestrict(
      contactPerson = contactPerson,
      restrictionType = repository.restrictionTypeOf(restrictionType),
      enteredStaff = enteredStaff,
      comment = comment,
      effectiveDate = effectiveDate,
      expiryDate = expiryDate,
    )
}
