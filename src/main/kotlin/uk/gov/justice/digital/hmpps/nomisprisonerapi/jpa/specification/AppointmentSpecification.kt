package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments.AppointmentFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIndividualSchedule

class AppointmentSpecification(private val filter: AppointmentFilter) : Specification<OffenderIndividualSchedule> {
  override fun toPredicate(
    root: Root<OffenderIndividualSchedule>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder,
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    predicates.add(criteriaBuilder.equal(root.get<String>(OffenderIndividualSchedule::eventType.name), "APP"))

    if (filter.prisonIds.isNotEmpty()) {
      predicates.add(
        criteriaBuilder.or(
          *filter.prisonIds.map {
            criteriaBuilder.equal(root.get<String>(OffenderIndividualSchedule::prison.name).get<String>(AgencyLocation::id.name), it)
          }.toTypedArray(),
        ),
      )
    }

    filter.fromDate?.run {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(OffenderIndividualSchedule::eventDate.name), this))
    }

    filter.toDate?.run {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(OffenderIndividualSchedule::eventDate.name), this))
    }

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
