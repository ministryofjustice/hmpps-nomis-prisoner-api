package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RestrictionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitorRestriction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class VisitorRestrictsDslMarker

@NomisDataDslMarker
interface VisitorRestrictsDsl

@Component
class VisitorRestrictsBuilderRepository(
  private val restrictionTypeRepository: ReferenceCodeRepository<RestrictionType>,
) {
  fun restrictionTypeOf(code: String): RestrictionType = restrictionTypeRepository.findByIdOrNull(RestrictionType.pk(code))!!
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
  ): VisitorRestriction =
    VisitorRestriction(
      person = person,
      restrictionType = repository.restrictionTypeOf(restrictionType),
      enteredStaff = enteredStaff,
      comment = comment,
      effectiveDate = effectiveDate,
      expiryDate = expiryDate,
    )
}
