package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderKeyDateAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderKeyDateAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository

@Service
@Transactional
class SentencingAdjustmentService(
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderSentenceAdjustmentRepository: OffenderSentenceAdjustmentRepository,
  private val offenderSentenceRepository: OffenderSentenceRepository,
  private val sentenceAdjustmentRepository: SentenceAdjustmentRepository,
  private val keyDateAdjustmentRepository: OffenderKeyDateAdjustmentRepository,
  private val entityManager: EntityManager,
  private val storedProcedureRepository: StoredProcedureRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun getSentenceAdjustment(adjustmentId: Long): SentenceAdjustmentResponse =
    offenderSentenceAdjustmentRepository.findByIdOrNull(adjustmentId)?.let {
      SentenceAdjustmentResponse(
        id = it.id,
        bookingId = it.offenderBooking.bookingId,
        sentenceSequence = it.sentenceSequence,
        adjustmentType = SentencingAdjustmentType(it.sentenceAdjustment.id, it.sentenceAdjustment.description),
        adjustmentDate = it.adjustmentDate,
        adjustmentFromDate = it.fromDate,
        adjustmentToDate = it.toDate,
        adjustmentDays = it.adjustmentNumberOfDays,
        comment = it.comment,
        active = it.active,
      )
    } ?: throw NotFoundException("Sentence adjustment $adjustmentId not found")

  fun createSentenceAdjustment(bookingId: Long, sentenceSequence: Long, request: CreateSentenceAdjustmentRequest) =
    offenderBookingRepository.findByIdOrNull(bookingId)?.let {
      offenderSentenceRepository.findByIdOrNull(SentenceId(it, sentenceSequence))?.let { sentence ->
        sentence.adjustments.add(
          OffenderSentenceAdjustment(
            offenderBooking = it,
            sentenceSequence = sentenceSequence,
            sentence = sentence,
            sentenceAdjustment = findValidSentenceAdjustmentType(request.adjustmentTypeCode),
            adjustmentDate = request.adjustmentDate,
            adjustmentNumberOfDays = request.adjustmentDays,
            fromDate = request.adjustmentFromDate,
            toDate = request.adjustmentFromDate?.plusDays(request.adjustmentDays - 1), // dates are inclusive so a 1-day remand starts and end on dame day
            comment = request.comment,
            active = request.active,
          )
        )
        entityManager.flush()
        CreateAdjustmentResponse(sentence.adjustments.last().id)
      }
        ?: throw NotFoundException("Sentence with sequence $sentenceSequence not found")
    } ?: throw NotFoundException("Booking $bookingId not found")

  fun updateSentenceAdjustment(adjustmentId: Long, request: UpdateSentenceAdjustmentRequest): Unit =
    offenderSentenceAdjustmentRepository.findByIdOrNull(adjustmentId)?.run {
      this.sentenceAdjustment = findValidSentenceAdjustmentType(request.adjustmentTypeCode)
      this.adjustmentDate = request.adjustmentDate
      this.adjustmentNumberOfDays = request.adjustmentDays
      this.fromDate = request.adjustmentFromDate
      this.toDate = request.adjustmentFromDate?.plusDays(request.adjustmentDays - 1)
      this.comment = request.comment
      this.active = request.active
    } ?: throw NotFoundException("Sentence adjustment with id $adjustmentId not found")

  fun deleteSentenceAdjustment(adjustmentId: Long) {
    offenderSentenceAdjustmentRepository.findByIdOrNull(adjustmentId)?.also {
      offenderSentenceAdjustmentRepository.deleteById(adjustmentId)
    } ?: {
      telemetryClient.trackEvent("sentence-adjustment-delete-not-found", mapOf("adjustmentId" to adjustmentId.toString()), null)
    }
  }

  private fun findValidSentenceAdjustmentType(adjustmentTypeCode: String) =
    sentenceAdjustmentRepository.findByIdOrNull(adjustmentTypeCode)?.also {
      if (!it.isSentenceRelated()) throw BadDataException("Sentence adjustment type $adjustmentTypeCode not valid for a sentence")
    }
      ?: throw BadDataException("Sentence adjustment type $adjustmentTypeCode not found")

  private fun findValidKeyDateAdjustmentType(adjustmentTypeCode: String) =
    sentenceAdjustmentRepository.findByIdOrNull(adjustmentTypeCode)?.also {
      if (!it.isBookingRelated()) throw BadDataException("Sentence adjustment type $adjustmentTypeCode not valid for a booking")
    }
      ?: throw BadDataException("Sentence adjustment type $adjustmentTypeCode not found")

  fun getKeyDateAdjustment(adjustmentId: Long): KeyDateAdjustmentResponse =
    keyDateAdjustmentRepository.findByIdOrNull(adjustmentId)?.let {
      KeyDateAdjustmentResponse(
        id = it.id,
        bookingId = it.offenderBooking.bookingId,
        adjustmentType = SentencingAdjustmentType(it.sentenceAdjustment.id, it.sentenceAdjustment.description),
        adjustmentDate = it.adjustmentDate,
        adjustmentFromDate = it.fromDate,
        adjustmentToDate = it.toDate,
        adjustmentDays = it.adjustmentNumberOfDays,
        comment = it.comment,
        active = it.active,
      )
    } ?: throw NotFoundException("Key date adjustment $adjustmentId not found")

  fun createKeyDateAdjustment(bookingId: Long, request: CreateKeyDateAdjustmentRequest) =
    offenderBookingRepository.findByIdOrNull(bookingId)?.let {
      it.keyDateAdjustments.add(
        OffenderKeyDateAdjustment(
          offenderBooking = it,
          sentenceAdjustment = findValidKeyDateAdjustmentType(request.adjustmentTypeCode),
          adjustmentDate = request.adjustmentDate,
          adjustmentNumberOfDays = request.adjustmentDays,
          fromDate = request.adjustmentFromDate,
          toDate = request.adjustmentFromDate.plusDays(request.adjustmentDays - 1),
          comment = request.comment,
          active = request.active,
        )
      )
      entityManager.flush()
      CreateAdjustmentResponse(it.keyDateAdjustments.last().id).also { createAdjustmentResponse ->
        storedProcedureRepository.postKeyDateAdjustmentCreation(
          keyDateAdjustmentId = createAdjustmentResponse.id,
          bookingId = bookingId
        )
      }
    } ?: throw NotFoundException("Booking $bookingId not found")

  fun updateKeyDateAdjustment(adjustmentId: Long, request: UpdateKeyDateAdjustmentRequest): Unit =
    keyDateAdjustmentRepository.findByIdOrNull(adjustmentId)?.run {
      this.sentenceAdjustment = findValidKeyDateAdjustmentType(request.adjustmentTypeCode)
      this.adjustmentDate = request.adjustmentDate
      this.adjustmentNumberOfDays = request.adjustmentDays
      this.fromDate = request.adjustmentFromDate
      this.toDate = request.adjustmentFromDate?.plusDays(request.adjustmentDays - 1)
      this.comment = request.comment
      this.active = request.active
      entityManager.flush()
      storedProcedureRepository.postKeyDateAdjustmentCreation(
        keyDateAdjustmentId = adjustmentId,
        bookingId = this.offenderBooking.bookingId
      )
    } ?: throw NotFoundException("Key date adjustment with id $adjustmentId not found")

  fun deleteKeyDateAdjustment(adjustmentId: Long) {
    keyDateAdjustmentRepository.findByIdOrNull(adjustmentId)?.run {
      keyDateAdjustmentRepository.deleteById(adjustmentId)
      entityManager.flush()
      storedProcedureRepository.postKeyDateAdjustmentCreation(
        keyDateAdjustmentId = adjustmentId,
        bookingId = this.offenderBooking.bookingId
      )
    } ?: {
      telemetryClient.trackEvent("key-date-adjustment-delete-not-found", mapOf("adjustmentId" to adjustmentId.toString()), null)
    }
  }

  fun findAdjustmentIdsByFilter(pageRequest: Pageable, adjustmentFilter: AdjustmentFilter): Page<AdjustmentIdResponse> {
    val adjustedToDate = adjustmentFilter.toDate?.let { adjustmentFilter.toDate.plusDays(1) }
    return keyDateAdjustmentRepository.adjustmentIdsQuery_named(
      fromDate = adjustmentFilter.fromDate,
      toDate = adjustedToDate,
      pageRequest
    )
  }
}
