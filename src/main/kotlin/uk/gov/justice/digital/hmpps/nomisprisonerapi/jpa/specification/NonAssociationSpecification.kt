package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations.NonAssociationFilter
import java.time.LocalDate

class NonAssociationSpecification(private val filter: NonAssociationFilter) : Specification<OffenderNonAssociation> {
  override fun toPredicate(
    root: Root<OffenderNonAssociation>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder,
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    val getEffectiveDate = root.get<LocalDate>(OffenderNonAssociation::offenderNonAssociationDetails.name)
      .get<LocalDate>(OffenderNonAssociationDetail::effectiveDate.name)

    filter.fromDate?.run {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(getEffectiveDate, this))
    }

    filter.toDate?.run {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(getEffectiveDate, this))
    }

    // Filter out duplicates by only returning the id - ordered record for each pair of offenders
    predicates.add(
        criteriaBuilder.lessThan(
            root.get<String>(OffenderNonAssociation::id.name).get<String>(OffenderNonAssociationId::offender.name),
            root.get<String>(OffenderNonAssociation::id.name).get<String>(OffenderNonAssociationId::nsOffender.name),
        ),
    )

    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
