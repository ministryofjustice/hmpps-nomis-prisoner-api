package uk.gov.justice.digital.hmpps.nomisprisonerapi.service

import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.resource.CreateSentenceAdjustmentRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.resource.CreateSentenceAdjustmentResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.resource.SentenceAdjustmentResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.resource.SentenceAdjustmentType

@Service
@Transactional
class SentenceAdjustmentService(
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderSentenceAdjustmentRepository: OffenderSentenceAdjustmentRepository,
  private val offenderSentenceRepository: OffenderSentenceRepository,
  private val sentenceAdjustmentRepository: SentenceAdjustmentRepository,
  private val entityManager: EntityManager
) {
  fun getSentenceAdjustment(sentenceAdjustmentId: Long): SentenceAdjustmentResponse =
    offenderSentenceAdjustmentRepository.findByIdOrNull(sentenceAdjustmentId)?.let {
      SentenceAdjustmentResponse(
        sentenceAdjustmentId = it.id,
        bookingId = it.offenderBooking.bookingId,
        sentenceSequence = it.sentenceSequence,
        sentenceAdjustmentType = SentenceAdjustmentType(it.sentenceAdjustment.id, it.sentenceAdjustment.description),
        adjustmentDate = it.adjustmentDate,
        adjustmentFromDate = it.fromDate,
        adjustmentToDate = it.toDate,
        adjustmentDays = it.adjustmentNumberOfDays,
        comment = it.comment,
        active = it.active,
      )
    } ?: throw NotFoundException("Sentence adjustment $sentenceAdjustmentId not found")

  fun createSentenceAdjustment(bookingId: Long, sentenceSequence: Long, request: CreateSentenceAdjustmentRequest) =
    offenderBookingRepository.findByIdOrNull(bookingId)?.let {
      offenderSentenceRepository.findByIdOrNull(SentenceId(it, sentenceSequence))?.let { sentence ->
        val sentenceAdjustment = sentenceAdjustmentRepository.findByIdOrNull(request.sentenceAdjustmentTypeCode)
          ?: throw BadDataException("Sentence adjustment type ${request.sentenceAdjustmentTypeCode} not found")
        sentence.adjustments.add(
          OffenderSentenceAdjustment(
            offenderBooking = it,
            sentenceSequence = sentenceSequence,
            sentence = sentence,
            sentenceAdjustment = sentenceAdjustment,
            adjustmentDate = request.adjustmentDate,
            adjustmentNumberOfDays = request.adjustmentDays,
            fromDate = request.adjustmentFromDate,
            toDate = request.adjustmentFromDate?.plusDays(request.adjustmentDays),
            comment = request.comment,
            active = request.active,
          )
        )
        entityManager.flush()
        CreateSentenceAdjustmentResponse(sentence.adjustments.last().id)
      }
        ?: throw NotFoundException("Sentence with sequence $sentenceSequence not found")
    } ?: throw NotFoundException("Booking $bookingId not found")
}
