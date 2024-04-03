package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport

class CSIPReportSpecification(private val filter: CSIPFilter) : Specification<CSIPReport> {
  override fun toPredicate(
    root: Root<CSIPReport>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder,
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    filter.fromDate?.run {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(CSIPReport::createDatetime.name), this.atStartOfDay()))
    }

    filter.toDate?.run {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(CSIPReport::createDatetime.name), this.plusDays(1).atStartOfDay()))
    }

    // TODO (when receive more info regarding migration)
    //  Determine if we need a customDTO for speed instead
    //  - see example with AdjudicationService/AdjudicationFilter
    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
