package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import org.springframework.lang.Nullable
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

class ActiveBookingsSpecification : Specification<OffenderBooking> {
  override fun toPredicate(
    root: Root<OffenderBooking>,
    @Nullable query: CriteriaQuery<*>?,
    criteriaBuilder: CriteriaBuilder,
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    predicates.add(criteriaBuilder.equal(root.get<String>(OffenderBooking::active.name), true))
    predicates.add(criteriaBuilder.isNull(root.get<String>(OffenderBooking::bookingEndDate.name)))

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
