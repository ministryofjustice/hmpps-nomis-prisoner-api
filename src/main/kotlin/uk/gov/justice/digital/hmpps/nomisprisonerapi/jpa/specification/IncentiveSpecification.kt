package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives.IncentiveFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive

class IncentiveSpecification(private val filter: IncentiveFilter) : Specification<Incentive> {
  override fun toPredicate(
    root: Root<Incentive>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    filter.fromDate?.run {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Incentive::whenCreated.name), this.atStartOfDay()))
    }

    filter.toDate?.run {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Incentive::whenCreated.name), this.plusDays(1).atStartOfDay()))
    }

    // TODO: latestOnly flag

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
