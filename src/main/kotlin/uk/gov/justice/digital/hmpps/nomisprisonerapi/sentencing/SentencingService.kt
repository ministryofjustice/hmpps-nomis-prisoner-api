package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ChargeStatusType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventChargeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.DirectionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LegalCaseType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenceResultCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceChargeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceTerm
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceTermId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationTypeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCategoryType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentencePurpose
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceTermType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtOrderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceResultCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderChargeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceTermRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceCalculationTypeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.ImprisonmentStatusChangeType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional
class SentencingService(
  private val courtCaseRepository: CourtCaseRepository,
  private val offenderSentenceRepository: OffenderSentenceRepository,
  private val offenderSentenceTermRepository: OffenderSentenceTermRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val telemetryClient: TelemetryClient,
  private val legalCaseTypeRepository: ReferenceCodeRepository<LegalCaseType>,
  private val sentenceTermTypeRepository: ReferenceCodeRepository<SentenceTermType>,
  private val sentenceCategoryRepository: ReferenceCodeRepository<SentenceCategoryType>,
  private val caseStatusRepository: ReferenceCodeRepository<CaseStatus>,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val eventStatusTypeRepository: ReferenceCodeRepository<EventStatus>,
  private val chargeStatusTypeRepository: ReferenceCodeRepository<ChargeStatusType>,
  private val movementReasonTypeRepository: ReferenceCodeRepository<MovementReason>,
  private val directionTypeRepository: ReferenceCodeRepository<DirectionType>,
  private val offenceRepository: OffenceRepository,
  private val offenderChargeRepository: OffenderChargeRepository,
  private val courtEventRepository: CourtEventRepository,
  private val offenceResultCodeRepository: OffenceResultCodeRepository,
  private val courtOrderRepository: CourtOrderRepository,
  private val storedProcedureRepository: StoredProcedureRepository,
  private val sentenceCalculationTypeRepository: SentenceCalculationTypeRepository,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val ACTIVE_CHARGE_STATUS = "A"
    private const val PARTIAL_RESULT_CODE_INDICATOR = "P"
    private const val FINAL_RESULT_CODE_INDICATOR = "F"
  }

  fun getCourtCase(id: Long, offenderNo: String): CourtCaseResponse {
    findLatestBooking(offenderNo)

    return courtCaseRepository.findByIdOrNull(id)?.toCourtCaseResponse()
      ?: throw NotFoundException("Court case $id not found")
  }

  fun getCourtCasesByOffender(offenderNo: String): List<CourtCaseResponse> {
    findLatestBooking(offenderNo)

    return courtCaseRepository.findByOffenderBookingOffenderNomsIdOrderByCreateDatetimeDesc(offenderNo)
      .map { courtCase ->
        courtCase.toCourtCaseResponse()
      }
  }

  fun getCourtCasesByOffenderBooking(bookingId: Long): List<CourtCaseResponse> {
    return findOffenderBooking(bookingId).let {
      courtCaseRepository.findByOffenderBookingOrderByCreateDatetimeDesc(it)
        .map { courtCase ->
          courtCase.toCourtCaseResponse()
        }
    }
  }

  fun getOffenderCharge(id: Long): OffenderCharge {
    return offenderChargeRepository.findByIdOrNull(id)
      ?: throw NotFoundException("Offender Charge $id not found")
  }

  @Audit
  fun createCourtCase(offenderNo: String, request: CreateCourtCaseRequest) =
    findLatestBooking(offenderNo).let { booking ->

      val courtCase = courtCaseRepository.saveAndFlush(
        CourtCase(
          offenderBooking = booking,
          legalCaseType = lookupLegalCaseType(request.legalCaseType),
          beginDate = request.startDate,
          caseStatus = lookupCaseStatus(request.status),
          court = lookupEstablishment(request.courtId),
          caseSequence = courtCaseRepository.getNextCaseSequence(booking),
        ),
      )
      courtCase.courtEvents.addAll(
        request.courtAppearance.let { courtAppearanceRequest ->
          mutableListOf(
            CourtEvent(
              offenderBooking = booking,
              courtCase = courtCase,
              eventDate = courtAppearanceRequest.eventDateTime.toLocalDate(),
              startTime = courtAppearanceRequest.eventDateTime,
              courtEventType = lookupMovementReasonType(courtAppearanceRequest.courtEventType),
              eventStatus = determineEventStatus(
                courtAppearanceRequest.eventDateTime.toLocalDate(),
                booking,
              ),
              court = lookupEstablishment(courtAppearanceRequest.courtId),
              outcomeReasonCode = courtAppearanceRequest.outcomeReasonCode?.let { lookupOffenceResultCode(it) },
              nextEventDate = courtAppearanceRequest.nextEventDateTime?.toLocalDate(),
              nextEventStartTime = courtAppearanceRequest.nextEventDateTime,
              directionCode = lookupDirectionType(DirectionType.OUT),
            ).also { courtEvent ->
              courtAppearanceRequest.courtEventChargesToCreate.map { offenderChargeRequest ->
                val resultCode =
                  offenderChargeRequest.resultCode1?.let { lookupOffenceResultCode(it) } ?: courtEvent.outcomeReasonCode
                // for the initial create - the duplicate fields on CourtEventCharge and OffenderCharge are identical
                val offenderCharge = OffenderCharge(
                  courtCase = courtCase,
                  offenderBooking = booking,
                  offence = lookupOffence(offenderChargeRequest.offenceCode),
                  offenceDate = offenderChargeRequest.offenceDate,
                  offenceEndDate = offenderChargeRequest.offenceEndDate,
                  // OCDCCASE offences taken into consideration
                  offencesCount = offenderChargeRequest.offencesCount,
                  resultCode1 = resultCode,
                  resultCode1Indicator = resultCode?.dispositionCode,
                  chargeStatus = resultCode?.chargeStatus?.let { lookupChargeStatusType(it) },
                )
                courtCase.offenderCharges.add(offenderCharge)
                courtCaseRepository.saveAndFlush(courtCase) // to access the newly created offender charges
              }
              courtEvent.initialiseCourtEventCharges()
            },
          )
        },
      )

      request.courtAppearance.nextEventDateTime?.let {
        courtCase.courtEvents.add(
          createNextCourtEvent(booking, courtCase.courtEvents[0], request.courtAppearance).also { nextCourtEvent ->
            nextCourtEvent.initialiseCourtEventCharges()
          },
        )
      }
      courtCaseRepository.saveAndFlush(courtCase).also {
        // TODO confirm no order associated with Next appearances
        refreshCourtOrder(courtEvent = courtCase.courtEvents[0], offenderNo = offenderNo)
        storedProcedureRepository.imprisonmentStatusUpdate(
          bookingId = booking.bookingId,
          changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
        )
      }
      CreateCourtCaseResponse(
        id = courtCase.id,
        courtAppearanceIds = courtCase.courtEvents.map
          {
            CreateCourtAppearanceResponse(
              id = it.id,
              courtEventChargesIds = it.courtEventCharges.map { courtEventCharge ->
                CreateCourtEventChargesResponse(
                  courtEventCharge.id.offenderCharge.id,
                )
              },
            )
          },
      ).also {
        telemetryClient.trackEvent(
          "court-case-created",
          mapOf(
            "courtCaseId" to courtCase.id.toString(),
            "bookingId" to booking.bookingId.toString(),
            "offenderNo" to offenderNo,
            "court" to request.courtId,
            "legalCaseType" to request.legalCaseType,
            "courtEventId" to it.courtAppearanceIds[0].id.toString(),
          ),
          null,
        )
      }
    }

  // TODO not currently updating the EXISTING offendercharge - eg with new offenceresultcode
  @Audit
  fun createCourtAppearance(
    offenderNo: String,
    caseId: Long,
    courtAppearanceRequest: CourtAppearanceRequest,
  ): CreateCourtAppearanceResponse {
    findLatestBooking(offenderNo).let { booking ->
      findCourtCase(caseId, offenderNo).let { courtCase ->
        val courtEvent = CourtEvent(
          offenderBooking = booking,
          courtCase = courtCase,
          eventDate = courtAppearanceRequest.eventDateTime.toLocalDate(),
          startTime = courtAppearanceRequest.eventDateTime,
          courtEventType = lookupMovementReasonType(courtAppearanceRequest.courtEventType),
          eventStatus = determineEventStatus(
            courtAppearanceRequest.eventDateTime.toLocalDate(),
            booking,
          ),
          court = lookupEstablishment(courtAppearanceRequest.courtId),
          outcomeReasonCode = courtAppearanceRequest.outcomeReasonCode?.let { lookupOffenceResultCode(it) },
          nextEventDate = courtAppearanceRequest.nextEventDateTime?.toLocalDate(),
          nextEventStartTime = courtAppearanceRequest.nextEventDateTime,
          directionCode = lookupDirectionType(DirectionType.OUT),
        )
        // 'to update' in this context of a new appearance means that the offender charges exist (not the court event charges)
        updateCharges(courtAppearanceRequest.courtEventChargesToUpdate, courtEvent)
        val createdOffenderCharges = createNewCharges(
          newCharges = courtAppearanceRequest.courtEventChargesToCreate,
          booking,
          courtCase,
          courtEvent,
        )

        courtCase.courtEvents.add(
          courtEvent,
        )
        courtEventRepository.saveAndFlush(courtEvent).let { createdCourtEvent ->
          var nextAppearanceId: Long? = null
          refreshCourtOrder(courtEvent = createdCourtEvent, offenderNo = offenderNo)
          createdCourtEvent.nextEventDate?.let {
            createdCourtEvent.courtCase!!.courtEvents.add(
              createNextCourtEvent(booking, createdCourtEvent, courtAppearanceRequest).also { nextCourtEvent ->
                nextCourtEvent.initialiseCourtEventCharges()
                nextAppearanceId = courtEventRepository.saveAndFlush(nextCourtEvent).id
              },
            )
          }
          storedProcedureRepository.imprisonmentStatusUpdate(
            bookingId = booking.bookingId,
            changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
          )
          return CreateCourtAppearanceResponse(
            id = createdCourtEvent.id,
            courtEventChargesIds = createdCourtEvent.courtEventCharges
              .filter { it.id.offenderCharge.id in createdOffenderCharges }
              .map { courtEventCharge ->
                CreateCourtEventChargesResponse(
                  courtEventCharge.id.offenderCharge.id,
                )
              },
            nextCourtAppearanceId = nextAppearanceId,
          ).also { response ->
            telemetryClient.trackEvent(
              "court-appearance-created",
              mapOf(
                "courtCaseId" to caseId.toString(),
                "bookingId" to booking.bookingId.toString(),
                "offenderNo" to offenderNo,
                "court" to courtAppearanceRequest.courtId,
                "courtEventId" to response.id.toString(),
                "nextCourtEventId" to response.nextCourtAppearanceId.toString(),
                "createdOffenderChargeIds" to createdOffenderCharges.toString(),
              ),
              null,
            )
          }
        }
      }
    }
  }

  fun getCourtAppearance(id: Long, offenderNo: String): CourtEventResponse {
    findLatestBooking(offenderNo)
    return findCourtAppearance(offenderNo = offenderNo, id = id).toCourtEvent()
  }

  private fun updateCharges(
    courtEventChargesToUpdate: List<ExistingOffenderChargeRequest>,
    courtEvent: CourtEvent,
  ) {
    courtEventChargesToUpdate.map { it ->
      getOffenderCharge(it.offenderChargeId).let { offenderCharge ->
        val resultCode = offenderCharge.resultCode1 ?: courtEvent.outcomeReasonCode
        courtEvent.courtEventCharges.add(
          CourtEventCharge(
            CourtEventChargeId(
              courtEvent = courtEvent,
              offenderCharge = offenderCharge,
            ),
            offenceDate = offenderCharge.offenceDate,
            offenceEndDate = offenderCharge.offenceEndDate,
            mostSeriousFlag = offenderCharge.mostSeriousFlag,
            offencesCount = offenderCharge.offencesCount,
            resultCode1 = resultCode,
            resultCode1Indicator = resultCode?.dispositionCode,
          ),
        )
      }
    }
  }

  private fun createNextCourtEvent(
    booking: OffenderBooking,
    courtEvent: CourtEvent,
    request: CourtAppearanceRequest,
  ) = CourtEvent(
    offenderBooking = booking,
    courtCase = courtEvent.courtCase,
    eventDate = courtEvent.nextEventDate!!,
    startTime = courtEvent.nextEventStartTime!!,
    courtEventType = courtEvent.courtEventType,
    // TODO confirm status rules are the same for next appearance
    eventStatus = determineEventStatus(
      courtEvent.nextEventDate!!,
      booking,
    ),
    // if next event, we must have a specified court
    court = lookupEstablishment(request.nextCourtId!!),
    directionCode = lookupDirectionType(DirectionType.OUT),
  )

  @Audit
  fun updateCourtAppearance(
    offenderNo: String,
    caseId: Long,
    eventId: Long,
    request: CourtAppearanceRequest,
  ): UpdateCourtAppearanceResponse {
    findLatestBooking(offenderNo).let { offenderBooking ->
      findCourtCase(caseId, offenderNo).let { courtCase ->
        findCourtAppearance(eventId, offenderNo).let { courtAppearance ->
          courtAppearance.eventDate = request.eventDateTime.toLocalDate()
          courtAppearance.startTime = request.eventDateTime
          courtAppearance.courtEventType = lookupMovementReasonType(request.courtEventType)
          courtAppearance.eventStatus = determineEventStatus(
            request.eventDateTime.toLocalDate(),
            courtCase.offenderBooking,
          )
          courtAppearance.court = lookupEstablishment(request.courtId)
          courtAppearance.outcomeReasonCode =
            request.outcomeReasonCode?.let { lookupOffenceResultCode(it) }
          // will get a separate update for a generated next event - so just updating these fields rather than the target appearance
          // todo confirm this is how updating next event dates will work
          courtAppearance.nextEventDate = request.nextEventDateTime?.toLocalDate()
          courtAppearance.nextEventStartTime = request.nextEventDateTime

          updateExistingCharges(chargesToUpdate = request.courtEventChargesToUpdate, courtAppearance)
          val createdOffenderCharges = createNewCharges(
            newCharges = request.courtEventChargesToCreate,
            offenderBooking,
            courtCase,
            courtAppearance,
          )

          val deletedOffenderCharges = courtCase.getOffenderChargesNotAssociatedWithCourtAppearances().also {
            it.forEach {
              courtCase.offenderCharges.remove(it)
              log.debug("Offender charge deleted: ${it.id}")
              telemetryClient.trackEvent(
                "offender-charge-deleted",
                mapOf(
                  "courtCaseId" to caseId.toString(),
                  "bookingId" to courtCase.offenderBooking.bookingId.toString(),
                  "offenderNo" to offenderNo,
                  "offenderChargeId" to it.id.toString(),
                  "courtEventId" to eventId.toString(),
                ),
                null,
              )
            }
          }

          courtEventRepository.saveAndFlush(courtAppearance).also {
            refreshCourtOrder(courtEvent = courtAppearance, offenderNo = offenderNo)
            storedProcedureRepository.imprisonmentStatusUpdate(
              bookingId = offenderBooking.bookingId,
              changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
            )
          }

          return UpdateCourtAppearanceResponse(
            createdCourtEventChargesIds = courtAppearance.courtEventCharges
              .filter { it.id.offenderCharge.id in createdOffenderCharges }
              .map { courtEventCharge ->
                CreateCourtEventChargesResponse(
                  courtEventCharge.id.offenderCharge.id,
                )
              },
            deletedOffenderChargesIds = deletedOffenderCharges.map { offenderCharge ->
              CreateCourtEventChargesResponse(
                offenderChargeId = offenderCharge.id,
              )
            },
          ).also {
            telemetryClient.trackEvent(
              "court-appearance-updated",
              mapOf(
                "courtCaseId" to caseId.toString(),
                "bookingId" to courtCase.offenderBooking.bookingId.toString(),
                "offenderNo" to offenderNo,
                "court" to request.courtId,
                "courtEventId" to eventId.toString(),
                "createdOffenderCharges" to it.createdCourtEventChargesIds.toString(),
                "deletedOffenderCharges" to it.deletedOffenderChargesIds.toString(),
              ),
              null,
            )
          }
        }
      }
    }
  }

  @Audit
  fun createSentence(offenderNo: String, request: CreateSentenceRequest) =
    findLatestBooking(offenderNo).let { booking ->

      val sentence = OffenderSentence(
        id = SentenceId(booking, sequence = offenderSentenceRepository.getNextSequence(booking)),
        category = lookupSentenceCategory(request.sentenceCategory),
        calculationType = lookupSentenceCalculationType(
          categoryCode = request.sentenceCategory,
          calcType = request.sentenceCalcType,
        ),
        courtCase = request.caseId?.let { findCourtCase(id = it, offenderNo = offenderNo) },
        startDate = request.startDate,
        endDate = request.endDate,
        status = request.status,
        fineAmount = request.fine,
        sentenceLevel = request.sentenceLevel,
        courtOrder = request.caseId?.let { existingCourtOrderByCaseId(it) },
      )
      sentence.offenderSentenceTerms.add(
        OffenderSentenceTerm(
          id = OffenderSentenceTermId(
            offenderBooking = booking,
            sentenceSequence = sentence.id.sequence,
            termSequence = offenderSentenceTermRepository.getNextTermSequence(
              offenderBookId = booking.bookingId,
              sentenceSeq = sentence.id.sequence,
            ),
          ),
          years = request.sentenceTerm.years,
          months = request.sentenceTerm.months,
          weeks = request.sentenceTerm.weeks,
          days = request.sentenceTerm.days,
          hours = request.sentenceTerm.hours,
          lifeSentenceFlag = request.sentenceTerm.lifeSentenceFlag,
          offenderSentence = sentence,
          startDate = request.sentenceTerm.startDate,
          endDate = request.sentenceTerm.endDate,
          sentenceTermType = lookupSentenceTermType(request.sentenceTerm.sentenceTermType),
        ),
      )

      sentence.offenderSentenceCharges.addAll(
        request.offenderChargeIds.map { chargeId ->
          OffenderSentenceCharge(
            id = OffenderSentenceChargeId(
              offenderBooking = booking,
              sequence = sentence.id.sequence,
              offenderChargeId = chargeId,
            ),
            offenderSentence = sentence,
            offenderCharge = findOffenderCharge(offenderNo = offenderNo, id = chargeId),
          )
        },
      )

      offenderSentenceRepository.saveAndFlush(sentence).also {
        storedProcedureRepository.imprisonmentStatusUpdate(
          bookingId = booking.bookingId,
          changeType = ImprisonmentStatusChangeType.UPDATE_SENTENCE.name,
        )
      }

      CreateSentenceResponse(
        sentenceSeq = sentence.id.sequence,
        termSeq = sentence.offenderSentenceTerms[0].id.termSequence,
      ).also { response ->
        telemetryClient.trackEvent(
          "sentence-created",
          mapOf(
            "sentenceSeq" to response.sentenceSeq.toString(),
            "termSeq" to response.termSeq.toString(),
            "bookingId" to booking.bookingId.toString(),
            "offenderNo" to offenderNo,
          ),
          null,
        )
      }
    }

  @Audit
  fun deleteSentence(bookingId: Long, sentenceSequence: Long) {
    offenderSentenceRepository.findByIdOrNull(
      SentenceId(
        offenderBooking = findOffenderBooking(bookingId),
        sequence = sentenceSequence,
      ),
    )?.also {
      offenderSentenceRepository.delete(it)
      telemetryClient.trackEvent(
        "sentence-deleted",
        mapOf(
          "bookingId" to it.id.offenderBooking.bookingId.toString(),
          "offenderNo" to it.id.offenderBooking.offender.nomsId,
          "sentenceSequence" to it.id.sequence.toString(),
        ),
        null,
      )
    }
      ?: telemetryClient.trackEvent(
        "sentence-delete-not-found",
        mapOf(
          "bookingId" to bookingId.toString(),
          "sentenceSequence" to sentenceSequence.toString(),
        ),
        null,
      )
  }

  @Audit
  fun updateSentence(
    bookingId: Long,
    sentenceSequence: Long,
    request: CreateSentenceRequest,
  ) {
    findOffenderBooking(bookingId).let { offenderBooking ->
      findSentence(booking = offenderBooking, sentenceSequence = sentenceSequence).let { sentence ->
        sentence.category = lookupSentenceCategory(request.sentenceCategory)
        sentence.calculationType = lookupSentenceCalculationType(
          categoryCode = request.sentenceCategory,
          calcType = request.sentenceCalcType,
        )
        sentence.courtCase =
          request.caseId?.let { findCourtCase(id = it, offenderNo = offenderBooking.offender.nomsId) }
        sentence.startDate = request.startDate
        sentence.endDate = request.endDate
        sentence.status = request.status
        sentence.fineAmount = request.fine
        sentence.sentenceLevel = request.sentenceLevel

        offenderSentenceRepository.saveAndFlush(sentence).also {
          storedProcedureRepository.imprisonmentStatusUpdate(
            bookingId = offenderBooking.bookingId,
            changeType = ImprisonmentStatusChangeType.UPDATE_SENTENCE.name,
          )
        }

        telemetryClient.trackEvent(
          "sentence-updated",
          mapOf(
            "bookingId" to bookingId.toString(),
            "sentenceSequence" to sentenceSequence.toString(),
          ),
          null,
        )
      }
    }
  }

  private fun updateExistingCharges(
    chargesToUpdate: List<ExistingOffenderChargeRequest>,
    courtAppearance: CourtEvent,
  ) {
    val chargesToUpdateMap = chargesToUpdate.map { it.offenderChargeId to it }.toMap()
    courtAppearance.courtEventCharges.filter { chargesToUpdateMap.contains(it.id.offenderCharge.id) }
      .map { courtEventCharge ->
        val offenderChargeRequest = chargesToUpdateMap.getValue(courtEventCharge.id.offenderCharge.id)
        val resultCode = offenderChargeRequest.resultCode1?.let { rs -> lookupOffenceResultCode(rs) }
        courtEventCharge.offenceDate = offenderChargeRequest.offenceDate
        courtEventCharge.offenceEndDate = offenderChargeRequest.offenceEndDate
        courtEventCharge.offencesCount = offenderChargeRequest.offencesCount
        courtEventCharge.resultCode1 = resultCode
        courtEventCharge.resultCode1Indicator = resultCode?.dispositionCode
        if (courtAppearance.isLatestAppearance()) {
          refreshOffenderCharge(courtEventCharge, offenderChargeRequest, resultCode)
        }
        courtEventCharge
      }.let {
        courtAppearance.courtEventCharges.clear()
        courtAppearance.courtEventCharges.addAll(it)
      }
  }

  private fun createNewCharges(
    newCharges: List<OffenderChargeRequest>,
    offenderBooking: OffenderBooking,
    courtCase: CourtCase,
    courtAppearance: CourtEvent,
  ): MutableList<Long> {
    val offenderChargeIds: MutableList<Long> = mutableListOf()
    newCharges.forEach { newCharge ->
      val resultCode = newCharge.resultCode1?.let { rs -> lookupOffenceResultCode(rs) }
      offenderChargeRepository.saveAndFlush(
        OffenderCharge(
          offenceDate = newCharge.offenceDate,
          offenceEndDate = newCharge.offenceEndDate,
          offencesCount = newCharge.offencesCount,
          offenderBooking = offenderBooking,
          resultCode1 = resultCode,
          resultCode1Indicator = resultCode?.dispositionCode,
          chargeStatus = resultCode?.let { lookupChargeStatusType(it.chargeStatus) },
          courtCase = courtCase,
          offence = lookupOffence(newCharge.offenceCode),
        ),
      ).let { offenderCharge ->
        offenderChargeIds.add(offenderCharge.id)
        CourtEventCharge(
          id = CourtEventChargeId(offenderCharge = offenderCharge, courtEvent = courtAppearance),
          offenceDate = newCharge.offenceDate,
          offenceEndDate = newCharge.offenceEndDate,
          offencesCount = newCharge.offencesCount,
          resultCode1 = resultCode,
          resultCode1Indicator = resultCode?.dispositionCode,
        ).let {
          courtAppearance.courtEventCharges.add(it)
        }
      }
    }
    return offenderChargeIds
  }

  private fun SentencingService.refreshOffenderCharge(
    courtEventCharge: CourtEventCharge,
    offenderChargeRequest: ExistingOffenderChargeRequest,
    resultCode: OffenceResultCode?,
  ) {
    with(courtEventCharge.id.offenderCharge) {
      offenceDate = offenderChargeRequest.offenceDate
      offenceEndDate = offenderChargeRequest.offenceEndDate
      offence = lookupOffence(offenderChargeRequest.offenceCode)
      resultCode1 = resultCode
      resultCode1Indicator = resultCode?.dispositionCode
      chargeStatus = resultCode?.chargeStatus?.let { lookupChargeStatusType(it) }
      offencesCount = offenderChargeRequest.offencesCount
    }
  }

  private fun refreshCourtOrder(
    courtEvent: CourtEvent,
    offenderNo: String,
  ) {
    if (courtEvent.courtEventCharges.any { charge -> charge.resultCode1?.resultRequiresACourtOrder() == true }) {
      existingCourtOrder(courtEvent.offenderBooking, courtEvent) ?: let {
        courtOrderRepository.save(
          CourtOrder(
            offenderBooking = courtEvent.offenderBooking,
            courtCase = courtEvent.courtCase!!,
            courtEvent = courtEvent,
            orderType = "AUTO",
            courtDate = courtEvent.eventDate,
            issuingCourt = courtEvent.court,
          ),
        )
        telemetryClient.trackEvent(
          "court-order-created",
          mapOf(
            "courtCaseId" to courtEvent.courtCase!!.id.toString(),
            "bookingId" to courtEvent.offenderBooking.bookingId.toString(),
            "offenderNo" to offenderNo,
            "court" to courtEvent.court.id,
            "courtEventId" to courtEvent.id.toString(),
          ),
          null,
        )
      }
    } else {
      existingCourtOrder(courtEvent.offenderBooking, courtEvent)?.let { courtOrder ->
        courtOrderRepository.delete(courtOrder)
        telemetryClient.trackEvent(
          "court-order-deleted",
          mapOf(
            "courtCaseId" to courtEvent.courtCase!!.id.toString(),
            "bookingId" to courtEvent.offenderBooking.bookingId.toString(),
            "offenderNo" to offenderNo,
            "court" to courtEvent.court.id,
            "courtEventId" to courtEvent.id.toString(),
            "courtOrderId" to courtOrder.id.toString(),
          ),
          null,
        )
      }
    }
  }

  fun OffenceResultCode.resultRequiresACourtOrder(): Boolean {
    log.info("Result code charge status: $this.chargeStatus and disposition code: ${this.dispositionCode}")
    log.info("resturning ${this.chargeStatus == ACTIVE_CHARGE_STATUS && (this.dispositionCode == PARTIAL_RESULT_CODE_INDICATOR || this.dispositionCode == FINAL_RESULT_CODE_INDICATOR)}")
    return this.chargeStatus == ACTIVE_CHARGE_STATUS && (this.dispositionCode == PARTIAL_RESULT_CODE_INDICATOR || this.dispositionCode == FINAL_RESULT_CODE_INDICATOR)
  }
  private fun existingCourtOrder(offenderBooking: OffenderBooking, courtEvent: CourtEvent) =
    courtOrderRepository.findByOffenderBookingAndCourtEventAndOrderType(offenderBooking, courtEvent)

  private fun existingCourtOrderByCaseId(caseId: Long) =
    courtOrderRepository.findFirstByCourtCase_IdAndOrderTypeOrderByCourtDateDesc(caseId = caseId)

  fun determineEventStatus(eventDate: LocalDate, booking: OffenderBooking): EventStatus {
    return if (eventDate < booking.bookingBeginDate.toLocalDate()
        .plusDays(1)
    ) {
      lookupEventStatusType(EventStatus.COMPLETED)
    } else {
      booking.externalMovements.maxByOrNull { it.id.sequence }?.let { lastMovement ->
        if (eventDate < lastMovement.movementDate) {
          lookupEventStatusType(EventStatus.COMPLETED)
        } else {
          lookupEventStatusType(
            EventStatus.SCHEDULED,
          )
        }
      } ?: lookupEventStatusType(
        EventStatus.SCHEDULED,
      )
    }
  }

  fun getOffenderSentence(sentenceSequence: Long, bookingId: Long): SentenceResponse {
    val offenderBooking = findOffenderBooking(bookingId)

    return offenderSentenceRepository.findByIdOrNull(
      SentenceId(
        offenderBooking = offenderBooking,
        sequence = sentenceSequence,
      ),
    )?.let { sentence ->
      SentenceResponse(
        bookingId = sentence.id.offenderBooking.bookingId,
        sentenceSeq = sentence.id.sequence,
        status = sentence.status,
        calculationType = sentence.calculationType.id.calculationType,
        startDate = sentence.startDate,
        courtOrder = sentence.courtOrder?.toCourtOrder(),
        consecSequence = sentence.consecSequence,
        endDate = sentence.endDate,
        commentText = sentence.commentText,
        absenceCount = sentence.absenceCount,
        caseId = sentence.courtCase?.id,
        etdCalculatedDate = sentence.etdCalculatedDate,
        mtdCalculatedDate = sentence.mtdCalculatedDate,
        ltdCalculatedDate = sentence.ltdCalculatedDate,
        ardCalculatedDate = sentence.ardCalculatedDate,
        crdCalculatedDate = sentence.crdCalculatedDate,
        pedCalculatedDate = sentence.pedCalculatedDate,
        npdCalculatedDate = sentence.npdCalculatedDate,
        ledCalculatedDate = sentence.ledCalculatedDate,
        sedCalculatedDate = sentence.sedCalculatedDate,
        prrdCalculatedDate = sentence.prrdCalculatedDate,
        tariffCalculatedDate = sentence.tariffCalculatedDate,
        dprrdCalculatedDate = sentence.dprrdCalculatedDate,
        tusedCalculatedDate = sentence.tusedCalculatedDate,
        aggSentenceSequence = sentence.aggSentenceSequence,
        aggAdjustDays = sentence.aggAdjustDays,
        sentenceLevel = sentence.sentenceLevel,
        extendedDays = sentence.extendedDays,
        counts = sentence.counts,
        statusUpdateReason = sentence.statusUpdateReason,
        statusUpdateComment = sentence.statusUpdateComment,
        statusUpdateDate = sentence.statusUpdateDate,
        statusUpdateStaffId = sentence.statusUpdateStaff?.id,
        category = sentence.category.toCodeDescription(),
        fineAmount = sentence.fineAmount,
        dischargeDate = sentence.dischargeDate,
        nomSentDetailRef = sentence.nomSentDetailRef,
        nomConsToSentDetailRef = sentence.nomConsToSentDetailRef,
        nomConsFromSentDetailRef = sentence.nomConsFromSentDetailRef,
        nomConsWithSentDetailRef = sentence.nomConsWithSentDetailRef,
        lineSequence = sentence.lineSequence,
        hdcExclusionFlag = sentence.hdcExclusionFlag,
        hdcExclusionReason = sentence.hdcExclusionReason,
        cjaAct = sentence.cjaAct,
        sled2Calc = sentence.sled2Calc,
        startDate2Calc = sentence.startDate2Calc,
        createdDateTime = sentence.createDatetime,
        createdByUsername = sentence.createUsername,
        sentenceTerms = sentence.offenderSentenceTerms.map { it.toSentenceTermResponse() },
        offenderCharges = sentence.offenderSentenceCharges.map { it.offenderCharge.toOffenderCharge() },
      )
    }
      ?: throw NotFoundException("Offender sentence for booking ${offenderBooking.bookingId} and sentence sequence $sentenceSequence not found")
  }

  private fun CourtEvent.initialiseCourtEventCharges() {
    this.courtEventCharges.addAll(
      this.courtCase!!.offenderCharges.map { offenderCharge ->
        CourtEventCharge(
          CourtEventChargeId(
            courtEvent = this,
            offenderCharge = offenderCharge,
          ),
          offenceDate = offenderCharge.offenceDate,
          offenceEndDate = offenderCharge.offenceEndDate,
          mostSeriousFlag = offenderCharge.mostSeriousFlag,
          offencesCount = offenderCharge.offencesCount,
          resultCode1 = offenderCharge.resultCode1,
          resultCode1Indicator = offenderCharge.resultCode1Indicator,
        )
      },
    )
  }

  fun getOffenderCharge(id: Long, offenderNo: String): OffenderChargeResponse {
    findLatestBooking(offenderNo)
    return findOffenderCharge(offenderNo = offenderNo, id = id).toOffenderCharge()
  }

  private fun findLatestBooking(offenderNo: String): OffenderBooking =
    offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Prisoner $offenderNo not found or has no bookings")

  private fun findOffenderBooking(id: Long): OffenderBooking {
    return offenderBookingRepository.findByIdOrNull(id)
      ?: throw NotFoundException("Offender booking $id not found")
  }

  private fun findCourtCase(id: Long, offenderNo: String): CourtCase {
    return courtCaseRepository.findByIdOrNull(id)
      ?: throw NotFoundException("Court case $id for $offenderNo not found")
  }

  private fun findCourtAppearance(id: Long, offenderNo: String): CourtEvent {
    return courtEventRepository.findByIdOrNull(id)
      ?: throw NotFoundException("Court appearance $id for $offenderNo not found")
  }

  private fun findSentence(sentenceSequence: Long, booking: OffenderBooking): OffenderSentence {
    return offenderSentenceRepository.findByIdOrNull(SentenceId(sequence = sentenceSequence, offenderBooking = booking))
      ?: throw NotFoundException("Sentence for booking ${booking.bookingId} and sentence sequence $sentenceSequence not found")
  }

  private fun findOffenderCharge(id: Long, offenderNo: String): OffenderCharge {
    return offenderChargeRepository.findByIdOrNull(id)
      ?: throw NotFoundException("Offender Charge $id for $offenderNo not found")
  }

  private fun lookupLegalCaseType(code: String): LegalCaseType = legalCaseTypeRepository.findByIdOrNull(
    LegalCaseType.pk(code),
  ) ?: throw BadDataException("Legal Case Type $code not found")

  private fun lookupSentenceTermType(code: String): SentenceTermType = sentenceTermTypeRepository.findByIdOrNull(
    SentenceTermType.pk(code),
  ) ?: throw BadDataException("Sentence Type $code not found")

  private fun lookupSentenceCategory(code: String): SentenceCategoryType = sentenceCategoryRepository.findByIdOrNull(
    SentenceCategoryType.pk(code),
  ) ?: throw BadDataException("Sentence category $code not found")

  private fun lookupSentenceCalculationType(categoryCode: String, calcType: String): SentenceCalculationType =
    sentenceCalculationTypeRepository.findByIdOrNull(
      SentenceCalculationTypeId(calculationType = calcType, category = categoryCode),
    )
      ?: throw BadDataException("Sentence calculation with category $categoryCode and calculation type $calcType not found")

  private fun lookupDirectionType(code: String): DirectionType = directionTypeRepository.findByIdOrNull(
    DirectionType.pk(code),
  ) ?: throw BadDataException("Case status $code not found")

  private fun lookupCaseStatus(code: String): CaseStatus = caseStatusRepository.findByIdOrNull(
    CaseStatus.pk(code),
  ) ?: throw BadDataException("Case status $code not found")

  private fun lookupEstablishment(courtId: String): AgencyLocation {
    return agencyLocationRepository.findByIdOrNull(courtId)
      ?: throw BadDataException("Establishment $courtId not found")
  }

  private fun lookupEventStatusType(code: String): EventStatus = eventStatusTypeRepository.findByIdOrNull(
    EventStatus.pk(code),
  ) ?: throw BadDataException("EventStatus Type $code not found")

  private fun lookupMovementReasonType(code: String): MovementReason = movementReasonTypeRepository.findByIdOrNull(
    MovementReason.pk(code),
  ) ?: throw BadDataException("Movement reason $code not found")

  private fun lookupOffence(offenceCode: String): Offence {
    val statuteCode = offenceCode.take(4)
    return offenceRepository.findByIdOrNull(OffenceId(offenceCode = offenceCode, statuteCode = statuteCode))
      ?: throw BadDataException("Offence with offence code $offenceCode: and statute code: $statuteCode not found")
  }

  private fun lookupOffenceResultCode(code: String): OffenceResultCode {
    return offenceResultCodeRepository.findByIdOrNull(code)
      ?: throw BadDataException("Offence result code $code not found")
  }

  private fun lookupChargeStatusType(code: String): ChargeStatusType = chargeStatusTypeRepository.findByIdOrNull(
    ChargeStatusType.pk(code),
  ) ?: throw BadDataException("Charge status Type $code not found")
}

private fun CourtCase.getOffenderChargesNotAssociatedWithCourtAppearances(): List<OffenderCharge> {
  val referencedOffenderCharges = this.courtEvents.flatMap { it.courtEventCharges.map { it.id.offenderCharge } }.toSet()
  return this.offenderCharges.filterNot { oc -> referencedOffenderCharges.contains(oc) }
}

private fun CourtCase.toCourtCaseResponse(): CourtCaseResponse = CourtCaseResponse(
  id = this.id,
  offenderNo = this.offenderBooking.offender.nomsId,
  bookingId = this.offenderBooking.bookingId,
  caseInfoNumber = this.caseInfoNumber,
  caseSequence = this.caseSequence,
  caseStatus = this.caseStatus.toCodeDescription(),
  legalCaseType = this.legalCaseType.toCodeDescription(),
  beginDate = this.beginDate,
  courtId = this.court.id,
  combinedCaseId = this.combinedCase?.id,
  lidsCaseNumber = this.lidsCaseNumber,
  lidsCaseId = this.lidsCaseId,
  lidsCombinedCaseId = this.lidsCombinedCaseId,
  statusUpdateReason = this.statusUpdateReason,
  statusUpdateComment = this.statusUpdateComment,
  statusUpdateDate = this.statusUpdateDate,
  statusUpdateStaffId = this.statusUpdateStaff?.id,
  createdDateTime = this.createDatetime,
  createdByUsername = this.createUsername,
  courtEvents = this.courtEvents.map { it.toCourtEvent() },
  offenderCharges = this.offenderCharges.map { it.toOffenderCharge() },
)

private fun OffenderCharge.toOffenderCharge(): OffenderChargeResponse = OffenderChargeResponse(
  id = this.id,
  offence = this.offence.toOffence(),
  offencesCount = this.offencesCount,
  offenceDate = this.offenceDate,
  offenceEndDate = this.offenceEndDate,
  plea = this.plea?.toCodeDescription(),
  propertyValue = this.propertyValue,
  totalPropertyValue = totalPropertyValue,
  cjitCode1 = this.cjitCode1,
  cjitCode2 = this.cjitCode2,
  cjitCode3 = this.cjitCode3,
  chargeStatus = this.chargeStatus?.toCodeDescription(),
  resultCode1 = this.resultCode1?.toCodeDescription(),
  resultCode2 = this.resultCode2?.toCodeDescription(),
  resultCode1Indicator = this.resultCode1Indicator,
  resultCode2Indicator = this.resultCode2Indicator,
  mostSeriousFlag = this.mostSeriousFlag,
  lidsOffenceNumber = this.lidsOffenceNumber,
)

private fun Offence.toOffence(): OffenceResponse = OffenceResponse(
  offenceCode = this.id.offenceCode,
  statuteCode = this.id.statuteCode,
  description = this.description,
)

private fun CourtEventCharge.toCourtEventCharge(): CourtEventChargeResponse =
  CourtEventChargeResponse(
    offenderCharge = this.id.offenderCharge.toOffenderCharge(),
    eventId = this.id.courtEvent.id,
    offencesCount = this.offencesCount,
    offenceDate = this.offenceDate,
    offenceEndDate = this.offenceEndDate,
    plea = this.plea?.toCodeDescription(),
    propertyValue = this.propertyValue,
    totalPropertyValue = totalPropertyValue,
    cjitCode1 = this.cjitCode1,
    cjitCode2 = this.cjitCode2,
    cjitCode3 = this.cjitCode3,
    resultCode1 = this.resultCode1?.toCodeDescription(),
    resultCode2 = this.resultCode2?.toCodeDescription(),
    resultCode1Indicator = this.resultCode1Indicator,
    resultCode2Indicator = this.resultCode2Indicator,
    mostSeriousFlag = this.mostSeriousFlag,
  )

private fun CourtEvent.toCourtEvent(): CourtEventResponse = CourtEventResponse(
  id = this.id,
  caseId = this.courtCase?.id,
  offenderNo = this.offenderBooking.offender.nomsId,
  eventDateTime = LocalDateTime.of(this.eventDate, this.startTime.toLocalTime()),
  courtEventType = this.courtEventType.toCodeDescription(),
  eventStatus = this.eventStatus.toCodeDescription(),
  directionCode = this.directionCode?.toCodeDescription(),
  judgeName = this.judgeName,
  courtId = this.court.id,
  outcomeReasonCode = this.outcomeReasonCode?.toCodeDescription(),
  commentText = this.commentText,
  orderRequestedFlag = this.orderRequestedFlag,
  holdFlag = this.holdFlag,
  nextEventRequestFlag = this.nextEventRequestFlag,
  nextEventDateTime = this.nextEventDate?.let {
    this.nextEventStartTime?.let {
      LocalDateTime.of(
        this.nextEventDate,
        this.nextEventStartTime!!.toLocalTime(),
      )
    } ?: LocalDateTime.of(this.nextEventDate, LocalTime.MIDNIGHT)
  },
  createdDateTime = this.createDatetime,
  createdByUsername = this.createUsername,
  courtEventCharges = this.courtEventCharges.map { it.toCourtEventCharge() },
  courtOrders = this.courtOrders.map { it.toCourtOrder() },
)

private fun CourtOrder.toCourtOrder(): CourtOrderResponse =
  CourtOrderResponse(
    id = this.id,
    courtDate = this.courtDate,
    issuingCourt = this.issuingCourt.id,
    courtInfoId = this.courtInfoId,
    orderType = this.orderType,
    orderStatus = this.orderStatus,
    dueDate = this.dueDate,
    requestDate = this.requestDate,
    seriousnessLevel = this.seriousnessLevel?.toCodeDescription(),
    commentText = this.commentText,
    nonReportFlag = this.nonReportFlag,
    sentencePurposes = this.sentencePurposes.map { it.toSentencePurpose() },
  )

private fun SentencePurpose.toSentencePurpose(): SentencePurposeResponse =
  SentencePurposeResponse(
    orderId = this.id.orderId,
    orderPartyCode = this.id.orderPartyCode,
    purposeCode = this.id.purposeCode,
  )

private fun OffenderSentenceTerm.toSentenceTermResponse(): SentenceTermResponse =
  SentenceTermResponse(
    termSequence = this.id.termSequence,
    years = this.years,
    months = this.months,
    weeks = this.weeks,
    days = this.days,
    hours = this.hours,
    startDate = this.startDate,
    endDate = this.endDate,
    lifeSentenceFlag = this.lifeSentenceFlag,
    sentenceTermType = this.sentenceTermType.toCodeDescription(),
  )
