package uk.gov.justice.digital.hmpps.nomisprisonerapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.resource.CreateSentenceAdjustmentRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.resource.CreateSentenceAdjustmentResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.resource.SentenceAdjustmentResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.resource.SentenceAdjustmentType
import java.time.LocalDate

@Service
@Transactional
class SentenceAdjustmentService {
  fun getSentenceAdjustment(sentenceAdjustmentId: Long): SentenceAdjustmentResponse = SentenceAdjustmentResponse(
    sentenceAdjustmentId = sentenceAdjustmentId,
    bookingId = 1,
    sentenceSequence = 1,
    sentenceAdjustmentType = SentenceAdjustmentType("UAL", "Unlawfully at Large"),
    adjustmentDate = LocalDate.now(),
    adjustmentFromDate = LocalDate.now(),
    adjustmentToDate = LocalDate.now().plusDays(10),
    adjustmentDays = 10,
    comment = "Adjustment comment",
    active = true,
  )

  fun createSentenceAdjustment(bookingId: Long, sentenceSequence: Long, request: CreateSentenceAdjustmentRequest) =
    CreateSentenceAdjustmentResponse(1L)
}
