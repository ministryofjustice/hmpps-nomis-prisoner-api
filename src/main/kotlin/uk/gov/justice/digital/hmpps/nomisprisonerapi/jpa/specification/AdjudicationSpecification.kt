package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications.AdjudicationFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation

class AdjudicationSpecification(private val filter: AdjudicationFilter) : Specification<AdjudicationIncidentParty> {
  override fun toPredicate(
    root: Root<AdjudicationIncidentParty>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder,
  ): Predicate? {
    // only consider parties that have an adjudicationNumber
    val predicates = mutableListOf<Predicate>(criteriaBuilder.isNotNull(root.get<Long>(AdjudicationIncidentParty::adjudicationNumber.name)))

    filter.fromDate?.run {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(AdjudicationIncidentParty::whenCreated.name), this.atStartOfDay()))
    }

    filter.toDate?.run {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(AdjudicationIncidentParty::whenCreated.name), this.plusDays(1).atStartOfDay()))
    }

    if (!filter.prisonIds.isNullOrEmpty()) {
      predicates.add(
        criteriaBuilder.or(
          *filter.prisonIds.map {
            criteriaBuilder.equal(
              root.get<String>(AdjudicationIncidentParty::incident.name)
                .get<String>(AdjudicationIncident::prison.name).get<String>(AgencyLocation::id.name),
              it,
            )
          }.toTypedArray(),
        ),
      )
    }

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
