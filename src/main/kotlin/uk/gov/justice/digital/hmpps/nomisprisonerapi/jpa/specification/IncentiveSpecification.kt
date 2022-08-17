package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.filter.IncentiveFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

class IncentiveSpecification(private val filter: IncentiveFilter) : Specification<Incentive> {
  override fun toPredicate(
    root: Root<Incentive>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    filter.fromDate?.run {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Incentive::iepDate.name), this))
    }

    filter.toDate?.run {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Incentive::iepDate.name), this))
    }

    // TODO: latestOnly flag

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
