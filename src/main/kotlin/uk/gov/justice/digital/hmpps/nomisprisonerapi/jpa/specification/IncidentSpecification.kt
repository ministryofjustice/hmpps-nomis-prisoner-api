package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents.IncidentFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident

class IncidentSpecification(private val filter: IncidentFilter) : Specification<Incident> {
  override fun toPredicate(
    root: Root<Incident>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder,
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    filter.fromDate?.run {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Incident::createDatetime.name), this.atStartOfDay()))
    }

    filter.toDate?.run {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Incident::createDatetime.name), this.plusDays(1).atStartOfDay()))
    }

    // TODO (when receive more info regarding migration)
    //  Determine if we need a customDTO for speed instead
    //  - see https://github.com/ministryofjustice/hmpps-nomis-prisoner-api/pull/577 for questionnaires
    //  - see example with AdjudicationService/AdjudicationFilter
    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
