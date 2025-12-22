package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.visits.VisitFilter
import java.time.LocalDate

class VisitSpecification(private val filter: VisitFilter) : Specification<Visit> {
  override fun toPredicate(
    root: Root<Visit>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder,
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    if (filter.visitTypes.isNotEmpty()) {
      predicates.add(
        criteriaBuilder.or(
          *filter.visitTypes.map {
            criteriaBuilder.equal(root.get<String>(Visit::visitType.name).get<String>(ReferenceCode::code.name), it)
          }.toTypedArray(),
        ),
      )
    }

    if (filter.prisonIds.isNotEmpty()) {
      predicates.add(
        criteriaBuilder.or(
          *filter.prisonIds.map {
            criteriaBuilder.equal(root.get<String>(Visit::location.name).get<String>(AgencyLocation::id.name), it)
          }.toTypedArray(),
        ),
      )
    }

    filter.fromDateTime?.run {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Visit::createDatetime.name), this))
    }

    filter.toDateTime?.run {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Visit::createDatetime.name), this))
    }

    filter.futureVisits?.takeIf { it }?.run {
      predicates.add(criteriaBuilder.greaterThan(root.get(Visit::visitDate.name), LocalDate.now()))
    }

    filter.excludeExtremeFutureDates?.takeIf { it }?.run {
      /* if we only interested in future dates, exclude any erroneous dates that are way in the future. There are a number of live visits
         with clearly invalid dates that are years in the future.
       */
      predicates.add(criteriaBuilder.lessThan(root.get(Visit::visitDate.name), LocalDate.now().plusYears(1)))
    }

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
