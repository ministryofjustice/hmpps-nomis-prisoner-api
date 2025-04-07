package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import org.springframework.lang.Nullable
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

class ActiveBookingsSpecification(val prisonId: String?) : Specification<OffenderBooking> {
  override fun toPredicate(
    root: Root<OffenderBooking>,
    @Nullable query: CriteriaQuery<*>?,
    criteriaBuilder: CriteriaBuilder,
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    predicates.add(criteriaBuilder.equal(root.get<String>(OffenderBooking::active.name), true))
    if (prisonId != null) {
      predicates.add(criteriaBuilder.equal(root.get<String>(OffenderBooking::location.name).get<String>(AgencyLocation::id.name), prisonId))
    }
    // ignore prisoners that are out but with a booking end date - since the data must be in a bad state
    // due to NOMIS data fix
    predicates.add(
      criteriaBuilder.not(
        criteriaBuilder.and(
          criteriaBuilder.isNotNull(root.get<String>(OffenderBooking::bookingEndDate.name)),
          criteriaBuilder.equal(root.get<String>(OffenderBooking::inOutStatus.name), "OUT"),
        ),
      ),
    )

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
