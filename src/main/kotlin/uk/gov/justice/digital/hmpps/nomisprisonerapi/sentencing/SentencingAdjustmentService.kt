package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
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

  @Audit
  fun createSentenceAdjustment(bookingId: Long, sentenceSequence: Long, request: CreateSentenceAdjustmentRequest) =
    offenderBookingRepository.findByIdOrNull(bookingId)?.let { booking ->
      offenderSentenceRepository.findByIdOrNull(SentenceId(booking, sentenceSequence))?.let { sentence ->
        sentence.adjustments.add(
          OffenderSentenceAdjustment(
            offenderBooking = booking,
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
        val adjustmentId = sentence.adjustments.last().id
        telemetryClient.trackEvent(
          "sentence-adjustment-created",
          mapOf(
            "bookingId" to bookingId.toString(),
            "offenderNo" to booking.offender.nomsId,
            "sentenceSequence" to sentenceSequence.toString(),
            "adjustmentId" to adjustmentId.toString(),
            "adjustmentType" to request.adjustmentTypeCode,
          ),
          null
        )
        CreateAdjustmentResponse(adjustmentId)
      }
        ?: throw NotFoundException("Sentence with sequence $sentenceSequence not found")
    } ?: throw NotFoundException("Booking $bookingId not found")

  @Audit
  fun updateSentenceAdjustment(adjustmentId: Long, request: UpdateSentenceAdjustmentRequest): Unit =
    offenderSentenceAdjustmentRepository.findByIdOrNull(adjustmentId)?.run {
      this.sentenceAdjustment = findValidSentenceAdjustmentType(request.adjustmentTypeCode)
      this.adjustmentDate = request.adjustmentDate
      this.adjustmentNumberOfDays = request.adjustmentDays
      this.fromDate = request.adjustmentFromDate
      this.toDate = request.adjustmentFromDate?.plusDays(request.adjustmentDays - 1)
      this.comment = request.comment
      this.active = request.active
      telemetryClient.trackEvent(
        "sentence-adjustment-updated",
        mapOf(
          "bookingId" to this.offenderBooking.bookingId.toString(),
          "offenderNo" to this.offenderBooking.offender.nomsId,
          "sentenceSequence" to this.sentenceSequence.toString(),
          "adjustmentId" to adjustmentId.toString(),
          "adjustmentType" to this.sentenceAdjustment.id,
        ),
        null
      )
    } ?: throw NotFoundException("Sentence adjustment with id $adjustmentId not found")

  @Audit
  fun deleteSentenceAdjustment(adjustmentId: Long) {
    offenderSentenceAdjustmentRepository.findByIdOrNull(adjustmentId)?.also {
      offenderSentenceAdjustmentRepository.deleteById(adjustmentId)
      telemetryClient.trackEvent(
        "sentence-adjustment-deleted",
        mapOf(
          "bookingId" to it.offenderBooking.bookingId.toString(),
          "offenderNo" to it.offenderBooking.offender.nomsId,
          "sentenceSequence" to it.sentenceSequence.toString(),
          "adjustmentId" to adjustmentId.toString(),
          "adjustmentType" to it.sentenceAdjustment.id,
        ),
        null
      )
    }
      ?: telemetryClient.trackEvent(
        "sentence-adjustment-delete-not-found",
        mapOf("adjustmentId" to adjustmentId.toString()),
        null
      )
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

  @Audit
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
      val adjustmentId = it.keyDateAdjustments.last().id
      telemetryClient.trackEvent(
        "key-date-adjustment-created",
        mapOf(
          "bookingId" to bookingId.toString(),
          "offenderNo" to it.offender.nomsId,
          "adjustmentId" to adjustmentId.toString(),
          "adjustmentType" to request.adjustmentTypeCode,
        ),
        null
      )
      CreateAdjustmentResponse(adjustmentId).also { createAdjustmentResponse ->
        storedProcedureRepository.postKeyDateAdjustmentUpsert(
          keyDateAdjustmentId = createAdjustmentResponse.id,
          bookingId = bookingId
        )
      }
    } ?: throw NotFoundException("Booking $bookingId not found")

  @Audit
  fun updateKeyDateAdjustment(adjustmentId: Long, request: UpdateKeyDateAdjustmentRequest): Unit =
    keyDateAdjustmentRepository.findByIdOrNull(adjustmentId)?.run {
      this.sentenceAdjustment = findValidKeyDateAdjustmentType(request.adjustmentTypeCode)
      this.adjustmentDate = request.adjustmentDate
      this.adjustmentNumberOfDays = request.adjustmentDays
      this.fromDate = request.adjustmentFromDate
      this.toDate = request.adjustmentFromDate.plusDays(request.adjustmentDays - 1)
      this.comment = request.comment
      this.active = request.active
      entityManager.flush()
      storedProcedureRepository.postKeyDateAdjustmentUpsert(
        keyDateAdjustmentId = adjustmentId,
        bookingId = this.offenderBooking.bookingId
      )
      telemetryClient.trackEvent(
        "key-date-adjustment-updated",
        mapOf(
          "bookingId" to this.offenderBooking.bookingId.toString(),
          "offenderNo" to this.offenderBooking.offender.nomsId,
          "adjustmentId" to adjustmentId.toString(),
          "adjustmentType" to request.adjustmentTypeCode,
        ),
        null
      )
    } ?: throw NotFoundException("Key date adjustment with id $adjustmentId not found")

  @Audit
  fun deleteKeyDateAdjustment(adjustmentId: Long) {
    keyDateAdjustmentRepository.findByIdOrNull(adjustmentId)?.also {
      storedProcedureRepository.preKeyDateAdjustmentDeletion(
        keyDateAdjustmentId = adjustmentId,
        bookingId = it.offenderBooking.bookingId
      )
      keyDateAdjustmentRepository.deleteById(adjustmentId)
      telemetryClient.trackEvent(
        "key-date-adjustment-deleted",
        mapOf(
          "bookingId" to it.offenderBooking.bookingId.toString(),
          "offenderNo" to it.offenderBooking.offender.nomsId,
          "adjustmentId" to adjustmentId.toString(),
          "adjustmentType" to it.sentenceAdjustment.id,
        ),
        null
      )
    } ?: telemetryClient.trackEvent(
      "key-date-adjustment-delete-not-found",
      mapOf("adjustmentId" to adjustmentId.toString()),
      null
    )
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
