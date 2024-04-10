package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

class OffenderWithBookingsSpecification : Specification<Offender> {
  override fun toPredicate(
    root: Root<Offender>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder,
  ): Predicate? {
    fun CriteriaBuilder.whereHasBookingWithSequenceOne() = equal(root.get<String>(Offender::bookings.name).get<String>(OffenderBooking::bookingSequence.name), 1)
    return criteriaBuilder.and(criteriaBuilder.whereHasBookingWithSequenceOne())
  }
}
