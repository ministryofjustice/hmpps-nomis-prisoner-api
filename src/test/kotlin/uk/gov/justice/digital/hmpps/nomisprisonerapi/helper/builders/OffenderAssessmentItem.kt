package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Assessment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessmentItem
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessmentItemId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAssessmentItemRepository
import java.math.BigDecimal

@DslMarker
annotation class OffenderAssessmentItemDslMarker

@NomisDataDslMarker
interface OffenderAssessmentItemDsl

@Component
class OffenderAssessmentItemBuilderRepository(
  private val offenderAssessmentItemRepository: OffenderAssessmentItemRepository,
) {
  fun save(offenderAssessmentItem: OffenderAssessmentItem): OffenderAssessmentItem = offenderAssessmentItemRepository
    .saveAndFlush(offenderAssessmentItem)
}

@Component
class OffenderAssessmentItemBuilderFactory(
  val repository: OffenderAssessmentItemBuilderRepository,
) {
  fun builder() = OffenderAssessmentItemBuilder(repository)
}

class OffenderAssessmentItemBuilder(
  private val repository: OffenderAssessmentItemBuilderRepository,
) : OffenderAssessmentItemDsl {
  lateinit var assessmentItem: OffenderAssessmentItem

  fun build(
    booking: OffenderBooking,
    sequence: Int,
    itemSequence: Int,
    offenderAssessment: OffenderAssessment,
    assessment: Assessment,
  ): OffenderAssessmentItem = OffenderAssessmentItem(
    id = OffenderAssessmentItemId(booking, sequence, itemSequence),
    score = BigDecimal.valueOf(1000),
    assessment = assessment,
    parentAssessment = assessment.parentAssessment,
    offenderAssessment = offenderAssessment,
    comment = "Item comment for sequence $sequence, item $itemSequence",
  )
    .let {
      assessmentItem = repository.save(it)
      return assessmentItem
    }
}
