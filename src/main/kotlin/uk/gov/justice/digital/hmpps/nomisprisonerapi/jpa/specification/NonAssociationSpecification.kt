package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations.NonAssociationFilter

class NonAssociationSpecification(private val filter: NonAssociationFilter) : Specification<OffenderNonAssociation> {
  override fun toPredicate(
    root: Root<OffenderNonAssociation>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder,
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    filter.fromDate?.run {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(OffenderNonAssociation::getEffectiveDate.name), this))
    }

    filter.toDate?.run {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(OffenderNonAssociation::getEffectiveDate.name), this))
    }

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}

fun OffenderNonAssociation.getEffectiveDate(): java.time.LocalDate? {
  return this.getOpenNonAssociationDetail()?.effectiveDate
}
