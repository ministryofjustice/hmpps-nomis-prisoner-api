package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents.QuestionnaireFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire

class QuestionnaireSpecification(private val filter: QuestionnaireFilter) : Specification<Questionnaire> {
  override fun toPredicate(
    root: Root<Questionnaire>,
    query: CriteriaQuery<*>,
    criteriaBuilder: CriteriaBuilder,
  ): Predicate? {
    val predicates = mutableListOf<Predicate>()

    filter.fromDate?.run {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(Questionnaire::createDatetime.name), this.atStartOfDay()))
    }

    filter.toDate?.run {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(Questionnaire::createDatetime.name), this.plusDays(1).atStartOfDay()))
    }

    // TODO (when receive more info regarding migration)
    //  Determine if we need a customDTO for speed instead - see https://github.com/ministryofjustice/hmpps-nomis-prisoner-api/pull/577
    //  - see example with AdjudicationService/AdjudicationFilter
    return criteriaBuilder.and(*predicates.toTypedArray())
  }
}
