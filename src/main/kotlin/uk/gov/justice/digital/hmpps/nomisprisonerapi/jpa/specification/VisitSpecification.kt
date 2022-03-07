package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.filter.VisitFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

class VisitSpecification(private val filter: VisitFilter) : Specification<Visit> {
  override fun toPredicate(
    root: Root<Visit>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    if (filter.visitTypes.isNotEmpty()) predicates.add(
      criteriaBuilder.or(
        *filter.visitTypes.map {
          criteriaBuilder.equal(root.get<String>(Visit::visitType.name).get<String>(ReferenceCode::code.name), it)
        }.toTypedArray()
      )
    )

    if (filter.prisonIds.isNotEmpty()) predicates.add(
      criteriaBuilder.or(
        *filter.prisonIds.map {
          criteriaBuilder.equal(root.get<String>(Visit::location.name).get<String>(AgencyLocation::id.name), it)
        }.toTypedArray()
      )
    )

    filter.fromDateTime?.run {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Visit::whenCreated.name), this))
    }

    filter.toDateTime?.run {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Visit::whenCreated.name), this))
    }

    filter.ignoreMissingRoom?.run {
      predicates.add(criteriaBuilder.isNotNull(root.get<String>(Visit::agencyInternalLocation.name)))
    }

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
