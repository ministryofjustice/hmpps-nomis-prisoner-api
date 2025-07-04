package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderKeyDateAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment.Companion.RECALL_REMAND_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment.Companion.RECALL_TAGGED_BAIL_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment.Companion.REMAND_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceAdjustment.Companion.TAGGED_BAIL_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.hasBeenReleased
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderKeyDateAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository
import java.time.LocalDate

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
  fun getSentenceAdjustment(adjustmentId: Long): SentenceAdjustmentResponse = offenderSentenceAdjustmentRepository.findByIdOrNull(adjustmentId)?.toAdjustmentResponse()
    ?: throw NotFoundException("Sentence adjustment $adjustmentId not found")

  @Audit
  fun createSentenceAdjustment(bookingId: Long, sentenceSequence: Long, request: CreateSentenceAdjustmentRequest) = offenderBookingRepository.findByIdOrNull(bookingId)?.let { booking ->
    offenderSentenceRepository.findByIdOrNull(SentenceId(booking, sentenceSequence))?.let { sentence ->
      val adjustmentId = offenderSentenceAdjustmentRepository.save(
        OffenderSentenceAdjustment(
          offenderBooking = booking,
          sentenceSequence = sentenceSequence,
          sentence = sentence,
          sentenceAdjustment = findValidSentenceAdjustmentType(request.adjustmentTypeCode),
          adjustmentDate = request.adjustmentDate,
          adjustmentNumberOfDays = request.adjustmentDays,
          fromDate = request.adjustmentFromDate,
          toDate = request.adjustmentFromDate.asToDate(request.adjustmentDays),
          comment = request.comment,
          active = request.active,
        ),
      ).id
      telemetryClient.trackEvent(
        "sentence-adjustment-created",
        mapOf(
          "bookingId" to bookingId.toString(),
          "offenderNo" to booking.offender.nomsId,
          "sentenceSequence" to sentenceSequence.toString(),
          "adjustmentId" to adjustmentId.toString(),
          "adjustmentType" to request.adjustmentTypeCode,
        ),
        null,
      )
      CreateAdjustmentResponse(adjustmentId)
    }
      ?: throw NotFoundException("Sentence with sequence $sentenceSequence not found")
  } ?: throw NotFoundException("Booking $bookingId not found")

  @Audit
  fun updateSentenceAdjustment(adjustmentId: Long, request: UpdateSentenceAdjustmentRequest): Unit = offenderSentenceAdjustmentRepository.findByIdOrNull(adjustmentId)?.run {
    offenderSentenceRepository.findByIdOrNull(SentenceId(this.offenderBooking, request.sentenceSequence))?.let { sentence ->
      this.sentenceAdjustment = findValidSentenceAdjustmentType(request.adjustmentTypeCode)
      this.adjustmentDate = request.adjustmentDate
      this.adjustmentNumberOfDays = request.adjustmentDays
      this.fromDate = request.adjustmentFromDate
      this.toDate = request.adjustmentFromDate.asToDate(request.adjustmentDays)
      this.comment = request.comment
      request.active?.also {
        // only set when supplied - currently DPS never overwrite this
        this.active = it
      }
      this.sentenceSequence = sentence.id.sequence
      telemetryClient.trackEvent(
        "sentence-adjustment-updated",
        mapOf(
          "bookingId" to this.offenderBooking.bookingId.toString(),
          "offenderNo" to this.offenderBooking.offender.nomsId,
          "sentenceSequence" to request.sentenceSequence.toString(),
          "adjustmentId" to adjustmentId.toString(),
          "adjustmentType" to this.sentenceAdjustment.id,
        ),
        null,
      )
    }
      ?: throw NotFoundException("Sentence with sequence ${request.sentenceSequence} not found")
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
        null,
      )
    }
      ?: telemetryClient.trackEvent(
        "sentence-adjustment-delete-not-found",
        mapOf("adjustmentId" to adjustmentId.toString()),
        null,
      )
  }

  private fun findValidSentenceAdjustmentType(adjustmentTypeCode: String) = sentenceAdjustmentRepository.findByIdOrNull(adjustmentTypeCode)?.also {
    if (!it.isSentenceRelated()) throw BadDataException("Sentence adjustment type $adjustmentTypeCode not valid for a sentence")
  }
    ?: throw BadDataException("Sentence adjustment type $adjustmentTypeCode not found")

  private fun findValidKeyDateAdjustmentType(adjustmentTypeCode: String) = sentenceAdjustmentRepository.findByIdOrNull(adjustmentTypeCode)?.also {
    if (!it.isBookingRelated()) throw BadDataException("Sentence adjustment type $adjustmentTypeCode not valid for a booking")
  }
    ?: throw BadDataException("Sentence adjustment type $adjustmentTypeCode not found")

  fun getKeyDateAdjustment(adjustmentId: Long): KeyDateAdjustmentResponse = keyDateAdjustmentRepository.findByIdOrNull(adjustmentId)?.toAdjustmentResponse()
    ?: throw NotFoundException("Key date adjustment $adjustmentId not found")

  @Audit
  fun createKeyDateAdjustment(bookingId: Long, request: CreateKeyDateAdjustmentRequest) = offenderBookingRepository.findByIdOrNull(bookingId)?.let {
    val adjustmentId = keyDateAdjustmentRepository.save(
      OffenderKeyDateAdjustment(
        offenderBooking = it,
        sentenceAdjustment = findValidKeyDateAdjustmentType(request.adjustmentTypeCode),
        adjustmentDate = request.adjustmentDate,
        adjustmentNumberOfDays = request.adjustmentDays,
        fromDate = request.adjustmentFromDate,
        toDate = request.adjustmentFromDate.asToDate(request.adjustmentDays),
        comment = request.comment,
        active = request.active,
      ),
    ).id
    telemetryClient.trackEvent(
      "key-date-adjustment-created",
      mapOf(
        "bookingId" to bookingId.toString(),
        "offenderNo" to it.offender.nomsId,
        "adjustmentId" to adjustmentId.toString(),
        "adjustmentType" to request.adjustmentTypeCode,
      ),
      null,
    )
    CreateAdjustmentResponse(adjustmentId).also { createAdjustmentResponse ->
      storedProcedureRepository.postKeyDateAdjustmentUpsert(
        keyDateAdjustmentId = createAdjustmentResponse.id,
        bookingId = bookingId,
      )
    }
  } ?: throw NotFoundException("Booking $bookingId not found")

  @Audit
  fun updateKeyDateAdjustment(adjustmentId: Long, request: UpdateKeyDateAdjustmentRequest): Unit = keyDateAdjustmentRepository.findByIdOrNull(adjustmentId)?.run {
    this.sentenceAdjustment = findValidKeyDateAdjustmentType(request.adjustmentTypeCode)
    this.adjustmentDate = request.adjustmentDate
    this.adjustmentNumberOfDays = request.adjustmentDays
    this.fromDate = request.adjustmentFromDate
    this.toDate = request.adjustmentFromDate.asToDate(request.adjustmentDays)
    this.comment = request.comment
    request.active?.also {
      // only set when supplied - currently DPS never overwrite this
      this.active = it
    }
    entityManager.flush()
    storedProcedureRepository.postKeyDateAdjustmentUpsert(
      keyDateAdjustmentId = adjustmentId,
      bookingId = this.offenderBooking.bookingId,
    )
    telemetryClient.trackEvent(
      "key-date-adjustment-updated",
      mapOf(
        "bookingId" to this.offenderBooking.bookingId.toString(),
        "offenderNo" to this.offenderBooking.offender.nomsId,
        "adjustmentId" to adjustmentId.toString(),
        "adjustmentType" to request.adjustmentTypeCode,
      ),
      null,
    )
  } ?: throw NotFoundException("Key date adjustment with id $adjustmentId not found")

  @Audit
  fun deleteKeyDateAdjustment(adjustmentId: Long) {
    keyDateAdjustmentRepository.findByIdOrNull(adjustmentId)?.also {
      storedProcedureRepository.preKeyDateAdjustmentDeletion(
        keyDateAdjustmentId = adjustmentId,
        bookingId = it.offenderBooking.bookingId,
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
        null,
      )
    } ?: telemetryClient.trackEvent(
      "key-date-adjustment-delete-not-found",
      mapOf("adjustmentId" to adjustmentId.toString()),
      null,
    )
  }

  fun findAdjustmentIdsByFilter(pageRequest: Pageable, adjustmentFilter: AdjustmentFilter): Page<AdjustmentIdResponse> {
    val adjustedToDate = adjustmentFilter.toDate?.let { adjustmentFilter.toDate.plusDays(1) }
    return keyDateAdjustmentRepository.adjustmentIdsQueryNamed(
      fromDate = adjustmentFilter.fromDate,
      toDate = adjustedToDate,
      pageRequest,
    )
  }

  fun getActiveSentencingAdjustments(bookingId: Long) = offenderBookingRepository.findByIdOrNull(bookingId)?.let { offenderBooking ->
    SentencingAdjustmentsResponse(
      keyDateAdjustments = keyDateAdjustmentRepository.findByOffenderBookingAndActive(offenderBooking, true)
        .map { it.toAdjustmentResponse() },
      sentenceAdjustments = offenderSentenceAdjustmentRepository.findByOffenderBookingAndActiveAndOffenderKeyDateAdjustmentIdIsNull(
        offenderBooking,
        true,
      )
        .map { it.toAdjustmentResponse() },
    )
  } ?: throw NotFoundException("Booking $bookingId not found")

  fun getAllSentencingAdjustments(bookingId: Long) = offenderBookingRepository.findByIdOrNull(bookingId)?.let { offenderBooking ->
    SentencingAdjustmentsResponse(
      keyDateAdjustments = keyDateAdjustmentRepository.findByOffenderBooking(offenderBooking)
        .map { it.toAdjustmentResponse() },
      sentenceAdjustments = offenderSentenceAdjustmentRepository.findByOffenderBookingAndOffenderKeyDateAdjustmentIdIsNull(
        offenderBooking,
      )
        .map { it.toAdjustmentResponse() },
    )
  } ?: throw NotFoundException("Booking $bookingId not found")

  @Transactional(propagation = Propagation.MANDATORY)
  fun convertAdjustmentsToRecallEquivalents(sentences: List<OffenderSentence>) {
    convertAdjustments(
      sentences = sentences,
      sourceAdjustments = listOf(REMAND_CODE, TAGGED_BAIL_CODE),
      targetAdjustments = listOf(RECALL_REMAND_CODE, RECALL_TAGGED_BAIL_CODE),
    )
  }

  @Transactional(propagation = Propagation.MANDATORY)
  fun convertAdjustmentsToPreRecallEquivalents(sentences: List<OffenderSentence>) {
    convertAdjustments(
      sentences = sentences,
      sourceAdjustments = listOf(RECALL_REMAND_CODE, RECALL_TAGGED_BAIL_CODE),
      targetAdjustments = listOf(REMAND_CODE, TAGGED_BAIL_CODE),
    )
  }

  @Transactional(propagation = Propagation.MANDATORY)
  fun activateAllAdjustment(sentences: List<OffenderSentence>): List<SentenceIdAndAdjustments> = sentences.map { sentence ->
    val adjustmentIds = sentence.adjustments.map {
      // mutate and make active
      it.active = true
      it.id
    }
    SentenceIdAndAdjustments(
      sentenceId = sentence.id,
      adjustmentIds = adjustmentIds,
    )
  }

  private fun convertAdjustments(
    sentences: List<OffenderSentence>,
    sourceAdjustments: List<String>,
    targetAdjustments: List<String>,
  ) {
    check(sourceAdjustments.size == targetAdjustments.size) {
      "Source and target adjustments must be the same size"
    }

    sourceAdjustments.zip(targetAdjustments).forEach { (source, target) ->
      sentences.forEach { sentence ->
        sentence.adjustments
          .filter { it.sentenceAdjustment.id == source }
          .forEach { it.sentenceAdjustment = sentenceAdjustmentRepository.findByIdOrNull(target)!! }
      }
    }
  }
}

private fun OffenderKeyDateAdjustment.toAdjustmentResponse() = KeyDateAdjustmentResponse(
  id = this.id,
  bookingId = this.offenderBooking.bookingId,
  bookingSequence = this.offenderBooking.bookingSequence,
  adjustmentType = SentencingAdjustmentType(this.sentenceAdjustment.id, this.sentenceAdjustment.description),
  adjustmentDate = this.adjustmentDate,
  adjustmentFromDate = this.fromDate,
  adjustmentToDate = this.toDate,
  adjustmentDays = this.adjustmentNumberOfDays,
  comment = this.comment,
  active = this.active,
  offenderNo = this.offenderBooking.offender.nomsId,
  hasBeenReleased = this.offenderBooking.hasBeenReleased(),
  prisonId = this.offenderBooking.location.id,
)

private fun OffenderSentenceAdjustment.toAdjustmentResponse() = SentenceAdjustmentResponse(
  id = this.id,
  bookingId = this.offenderBooking.bookingId,
  bookingSequence = this.offenderBooking.bookingSequence,
  sentenceSequence = this.sentenceSequence,
  adjustmentType = SentencingAdjustmentType(this.sentenceAdjustment.id, this.sentenceAdjustment.description),
  adjustmentDate = this.adjustmentDate,
  adjustmentFromDate = this.fromDate,
  adjustmentToDate = this.toDate,
  adjustmentDays = this.adjustmentNumberOfDays,
  comment = this.comment,
  active = this.active,
  hiddenFromUsers = this.offenderKeyDateAdjustmentId != null,
  offenderNo = this.offenderBooking.offender.nomsId,
  hasBeenReleased = this.offenderBooking.hasBeenReleased(),
  prisonId = this.offenderBooking.location.id,
)

// dates are inclusive so a 1-day remand starts and end on dame day - unless zero days so have no toDate else it would be the day before
private fun LocalDate?.asToDate(adjustmentDays: Long) = this?.takeIf { adjustmentDays > 0 }?.plusDays(adjustmentDays - 1)

data class SentenceIdAndAdjustments(
  val sentenceId: SentenceId,
  val adjustmentIds: List<Long>,
)
