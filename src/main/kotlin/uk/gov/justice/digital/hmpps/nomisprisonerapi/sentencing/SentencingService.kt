package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseIdentifierType
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseIdentifierPK
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventChargeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtOrderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceResultCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCaseIdentifierRepository
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
  private val courtEventChargeRepository: CourtEventChargeRepository,
  private val courtEventRepository: CourtEventRepository,
  private val offenceResultCodeRepository: OffenceResultCodeRepository,
  private val courtOrderRepository: CourtOrderRepository,
  private val storedProcedureRepository: StoredProcedureRepository,
  private val sentenceCalculationTypeRepository: SentenceCalculationTypeRepository,
  private val caseIdentifierRepository: OffenderCaseIdentifierRepository,
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

  fun getCourtCaseForMigration(id: Long): CourtCaseResponse {
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
  fun createCourtCaseHierachy(offenderNo: String, request: CreateCourtCaseRequest) =
    findLatestBooking(offenderNo).let { booking ->

      val courtCase = courtCaseRepository.saveAndFlush(
        CourtCase(
          offenderBooking = booking,
          legalCaseType = lookupLegalCaseType(request.legalCaseType),
          beginDate = request.startDate,
          caseStatus = lookupCaseStatus(request.status),
          court = lookupEstablishment(request.courtId),
          caseSequence = courtCaseRepository.getNextCaseSequence(booking),
          primaryCaseInfoNumber = request.caseReference,
        ),
      )
      val mandatoryCourtAppearanceRequest = request.courtAppearance!!
      courtCase.courtEvents.addAll(
        mandatoryCourtAppearanceRequest.let { courtAppearanceRequest ->
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
              mandatoryCourtAppearanceRequest.courtEventChargesToCreate.map { offenderChargeRequest ->
                val resultCode =
                  offenderChargeRequest.resultCode1?.let { lookupOffenceResultCode(it) } ?: courtEvent.outcomeReasonCode
                // for the initial create - the duplicate fields on CourtEventCharge and OffenderCharge are identical
                val offenderCharge = OffenderCharge(
                  courtCase = courtCase,
                  offenderBooking = booking,
                  offence = lookupOffence(offenderChargeRequest.offenceCode),
                  offenceDate = offenderChargeRequest.offenceDate,
                  offenceEndDate = offenderChargeRequest.offenceEndDate,
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
      courtCaseRepository.saveAndFlush(courtCase).also {
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
                OffenderChargeIdResponse(
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
      courtCaseRepository.saveAndFlush(courtCase)
      CreateCourtCaseResponse(
        id = courtCase.id,
        courtAppearanceIds = emptyList(),
      ).also {
        telemetryClient.trackEvent(
          "court-case-created",
          mapOf(
            "courtCaseId" to courtCase.id.toString(),
            "bookingId" to booking.bookingId.toString(),
            "offenderNo" to offenderNo,
            "court" to request.courtId,
            "legalCaseType" to request.legalCaseType,
          ),
          null,
        )
      }
    }

  fun findCourtCaseIdsByFilter(pageRequest: Pageable, courtCaseFilter: CourtCaseFilter): Page<CourtCaseIdResponse> =
    if (courtCaseFilter.toDateTime == null && courtCaseFilter.fromDateTime == null) {
      courtCaseRepository.findAllCourtCaseIds(pageable = pageRequest).map { CourtCaseIdResponse(it) }
    } else {
      courtCaseRepository.findAllCourtCaseIds(
        fromDateTime = courtCaseFilter.fromDateTime,
        toDateTime = courtCaseFilter.toDateTime,
        pageable = pageRequest,
      ).map { CourtCaseIdResponse(it) }
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
        // TODO CHECK CORRECTNESS: this is using the underlying offender charge to set values like result code
        updateCharges(courtAppearanceRequest.courtEventChargesToUpdate, courtEvent)

        courtCase.courtEvents.add(
          courtEvent,
        )
        courtEventRepository.saveAndFlush(courtEvent).let { createdCourtEvent ->
          refreshCourtOrder(courtEvent = createdCourtEvent, offenderNo = offenderNo)
          storedProcedureRepository.imprisonmentStatusUpdate(
            bookingId = booking.bookingId,
            changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
          )
          return CreateCourtAppearanceResponse(
            id = createdCourtEvent.id,
          ).also { response ->
            telemetryClient.trackEvent(
              "court-appearance-created",
              mapOf(
                "courtCaseId" to caseId.toString(),
                "bookingId" to booking.bookingId.toString(),
                "offenderNo" to offenderNo,
                "court" to courtAppearanceRequest.courtId,
                "courtEventId" to response.id.toString(),
              ),
              null,
            )
          }
        }
      }
    }
  }

  // creates offender charge without associating with a Court Event
  fun createCourtCharge(
    offenderNo: String,
    caseId: Long,
    offenderChargeRequest: OffenderChargeRequest,
  ): OffenderChargeIdResponse {
    findLatestBooking(offenderNo).let { booking ->
      findCourtCase(caseId, offenderNo).let { courtCase ->
        val resultCode =
          offenderChargeRequest.resultCode1?.let { lookupOffenceResultCode(it) }
        val offenderCharge = OffenderCharge(
          courtCase = courtCase,
          offenderBooking = booking,
          offence = lookupOffence(offenderChargeRequest.offenceCode),
          offenceDate = offenderChargeRequest.offenceDate,
          offenceEndDate = offenderChargeRequest.offenceEndDate,
          resultCode1 = resultCode,
          resultCode1Indicator = resultCode?.dispositionCode,
          chargeStatus = resultCode?.chargeStatus?.let { lookupChargeStatusType(it) },
        )
        offenderChargeRepository.saveAndFlush(offenderCharge).let { createdOffenderCharge ->
          return OffenderChargeIdResponse(
            offenderChargeId = createdOffenderCharge.id,
          ).also { response ->
            // calculates main offence
            storedProcedureRepository.imprisonmentStatusUpdate(
              bookingId = booking.bookingId,
              changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
            )
            telemetryClient.trackEvent(
              "offender-charge-created",
              mapOf(
                "courtCaseId" to caseId.toString(),
                "bookingId" to booking.bookingId.toString(),
                "offenderNo" to offenderNo,
                "offenderChargeId" to response.offenderChargeId.toString(),
                "offenceCode" to createdOffenderCharge.offence.id.offenceCode,
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
            resultCode1 = resultCode,
            resultCode1Indicator = resultCode?.dispositionCode,
          ),
        )
      }
    }
  }

  // use offender charge to reset all CEC common values
  private fun refreshCourtEventCharges(
    offenderCharge: OffenderCharge,
    case: CourtCase,
  ) {
    case.courtEvents.flatMap { it.courtEventCharges }.filter { it.id.offenderCharge == offenderCharge }
      .forEach { courtEventCharge ->
        courtEventCharge.offenceDate = offenderCharge.offenceDate
        courtEventCharge.offenceEndDate = offenderCharge.offenceEndDate
        courtEventCharge.mostSeriousFlag = offenderCharge.mostSeriousFlag
        courtEventCharge.resultCode1 = offenderCharge.resultCode1
        courtEventCharge.resultCode1Indicator = offenderCharge.resultCode1Indicator
      }
  }

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
          courtAppearance.nextEventDate = request.nextEventDateTime?.toLocalDate()
          courtAppearance.nextEventStartTime = request.nextEventDateTime

          updateExistingCharges(chargesToUpdate = request.courtEventChargesToUpdate, courtAppearance)

          // Offender charges are deleted if no longer associated with an appearance
          val deletedOffenderCharges =
            courtCase.getOffenderChargesNotAssociatedWithCourtAppearances().also { orphanedOffenderCharges ->
              orphanedOffenderCharges.forEach {
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
            deletedOffenderChargesIds = deletedOffenderCharges.map { offenderCharge ->
              OffenderChargeIdResponse(
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
  fun deleteCourtAppearance(
    offenderNo: String,
    caseId: Long,
    eventId: Long,
  ) {
    findLatestBooking(offenderNo).let { booking ->
      val telemetry = mapOf(
        "bookingId" to booking.bookingId.toString(),
        "offenderNo" to offenderNo,
        "eventId" to eventId.toString(),
        "caseId" to caseId.toString(),
      )
      courtEventRepository.findByIdOrNull(eventId)?.also {
        courtEventRepository.delete(it)
        storedProcedureRepository.imprisonmentStatusUpdate(
          bookingId = booking.bookingId,
          changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
        )
        telemetryClient.trackEvent(
          "court-appearance-deleted",
          telemetry,
          null,
        )
      }
        ?: telemetryClient.trackEvent(
          "court-appearance-delete-not-found",
          telemetry,
          null,
        )
    }
  }

  @Audit
  fun deleteCourtCase(
    offenderNo: String,
    caseId: Long,
  ) {
    findLatestBooking(offenderNo).let { booking ->
      val telemetry = mapOf(
        "bookingId" to booking.bookingId.toString(),
        "offenderNo" to offenderNo,
        "caseId" to caseId.toString(),
      )
      courtCaseRepository.findByIdOrNull(caseId)?.also {
        courtCaseRepository.delete(it)
        storedProcedureRepository.imprisonmentStatusUpdate(
          bookingId = booking.bookingId,
          changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
        )
        telemetryClient.trackEvent(
          "court-case-deleted",
          telemetry,
          null,
        )
      }
        ?: telemetryClient.trackEvent(
          "court-case-delete-not-found",
          telemetry,
          null,
        )
    }
  }

  @Audit
  fun updateCourtCharge(
    offenderNo: String,
    caseId: Long,
    chargeId: Long,
    courtEventId: Long,
    request: OffenderChargeRequest,
  ) {
    findLatestBooking(offenderNo).let { booking ->
      findCourtCase(caseId, offenderNo).let { courtCase ->
        findCourtAppearance(courtEventId, offenderNo).let { courtAppearance ->
          val offenderCharge = findOffenderCharge(offenderNo = offenderNo, id = chargeId)
          findCourtEventCharge(
            offenderNo = offenderNo,
            id = CourtEventChargeId(offenderCharge, courtAppearance),
          ).let { courtEventCharge ->

            val resultCode = request.resultCode1?.let { lookupOffenceResultCode(it) }
            courtEventCharge.offenceDate = request.offenceDate
            courtEventCharge.offenceEndDate = request.offenceEndDate
            courtEventCharge.resultCode1 = resultCode
            courtEventCharge.resultCode1Indicator = resultCode?.dispositionCode

            refreshOffenderCharge(
              courtEventCharge = courtEventCharge,
              offenderChargeRequest = request.toExistingOffenderChargeRequest(chargeId),
              resultCode = resultCode,
              latestAppearance = courtEventCharge.id.courtEvent.isLatestAppearance(),
            ).also {
              courtCase.courtEvents.forEach { courtAppearance ->
                courtEventRepository.saveAndFlush(courtAppearance).also {
                  refreshCourtOrder(courtEvent = courtAppearance, offenderNo = offenderNo)
                }
              }
              storedProcedureRepository.imprisonmentStatusUpdate(
                bookingId = booking.bookingId,
                changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
              )
              telemetryClient.trackEvent(
                "court-charge-updated",
                mapOf(
                  "courtCaseId" to caseId.toString(),
                  "bookingId" to booking.bookingId.toString(),
                  "offenderNo" to offenderNo,
                  "offenderChargeId" to chargeId.toString(),
                ),
                null,
              )
            }
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
        courtEventCharge.resultCode1 = resultCode
        courtEventCharge.resultCode1Indicator = resultCode?.dispositionCode

        refreshOffenderCharge(courtEventCharge, offenderChargeRequest, resultCode, courtAppearance.isLatestAppearance())

        courtEventCharge
      }.let {
        courtAppearance.courtEventCharges.clear()
        courtAppearance.courtEventCharges.addAll(it)
      }
  }

  // nomis only updates the outcome/result on the Offence Charge if a change is made to the latest Court Event Charge
  private fun SentencingService.refreshOffenderCharge(
    courtEventCharge: CourtEventCharge,
    offenderChargeRequest: ExistingOffenderChargeRequest,
    resultCode: OffenceResultCode?,
    latestAppearance: Boolean = true,
  ) {
    with(courtEventCharge.id.offenderCharge) {
      offenceDate = offenderChargeRequest.offenceDate
      offenceEndDate = offenderChargeRequest.offenceEndDate
      if (latestAppearance) {
        resultCode1 = resultCode
        resultCode1Indicator = resultCode?.dispositionCode
        chargeStatus = resultCode?.chargeStatus?.let { lookupChargeStatusType(it) }
      }
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

  fun OffenceResultCode.resultRequiresACourtOrder(): Boolean =
    this.chargeStatus == ACTIVE_CHARGE_STATUS && (this.dispositionCode == PARTIAL_RESULT_CODE_INDICATOR || this.dispositionCode == FINAL_RESULT_CODE_INDICATOR)

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

  fun getCourtEventCharge(chargeId: Long, eventId: Long, offenderNo: String): CourtEventChargeResponse {
    val charge = getOffenderCharge(chargeId)
    val appearance = findCourtAppearance(offenderNo = offenderNo, id = eventId)
    return findCourtEventCharge(CourtEventChargeId(offenderCharge = charge, courtEvent = appearance), offenderNo = offenderNo).toCourtEventCharge()
  }

  fun refreshCaseIdentifiers(offenderNo: String, caseId: Long, request: CaseIdentifierRequest) {
    val courtCase = courtCaseRepository.findByIdOrNull(caseId)
      ?: throw NotFoundException("Court case $caseId not found")
    val existingDpsCaseIdentifiers = courtCase.getDpsCaseInfoNumbers()
    val requestCaseIdentifierReferences = request.caseIdentifiers.map { it.reference }

    val caseIdentifiersToRemove =
      existingDpsCaseIdentifiers.filter { it.id.reference !in request.caseIdentifiers.map { it.reference } }
    val caseIdentifiersToAdd =
      (requestCaseIdentifierReferences - existingDpsCaseIdentifiers.map { it.id.reference }).map {
        OffenderCaseIdentifier(
          id = OffenderCaseIdentifierPK(
            identifierType = CaseIdentifierType.DPS_CASE_REFERENCE,
            courtCase = courtCase,
            reference = it,
          ),

        )
      }

    log.info("Removing case identifiers for offender $offenderNo: ${caseIdentifiersToRemove.map { it.id }}")
    log.info("Adding case identifiers offender $offenderNo: ${caseIdentifiersToAdd.map { it.id }}")
    courtCase.caseInfoNumbers.removeAll(caseIdentifiersToRemove)
    courtCase.caseInfoNumbers.addAll(caseIdentifiersToAdd)
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

  private fun findCourtEventCharge(id: CourtEventChargeId, offenderNo: String): CourtEventCharge {
    return courtEventChargeRepository.findByIdOrNull(id)
      ?: throw NotFoundException("Court event charge with offenderChargeId ${id.offenderCharge.id} for $offenderNo not found")
  }

  private fun findLastModifiedCourtEventCharge(offenderChargeId: Long, offenderNo: String): CourtEventCharge {
    return courtEventChargeRepository.findFirstByIdOffenderChargeIdOrderByLastModifiedDateTimeDesc(offenderChargeId)
      .also { courtEventCharge ->
        log.info("BLA BLA BLA " + courtEventCharge?.id?.offenderCharge?.id.toString() + " ${courtEventCharge?.lastModifiedDateTime}")
      }
      ?: throw NotFoundException("Last modified Court event charge with offenderChargeId $offenderChargeId for $offenderNo not found")
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

private fun OffenderChargeRequest.toExistingOffenderChargeRequest(chargeId: Long): ExistingOffenderChargeRequest =
  ExistingOffenderChargeRequest(
    offenceCode = this.offenceCode,
    offenceDate = this.offenceDate,
    offenceEndDate = this.offenceEndDate,
    resultCode1 = this.resultCode1,
    offenderChargeId = chargeId,
  )

private fun CourtCase.getOffenderChargesNotAssociatedWithCourtAppearances(): List<OffenderCharge> {
  val referencedOffenderCharges =
    this.courtEvents.flatMap { courtEvent -> courtEvent.courtEventCharges.map { it.id.offenderCharge } }.toSet()
  return this.offenderCharges.filterNot { oc -> referencedOffenderCharges.contains(oc) }
}

private fun CourtCase.toCourtCaseResponse(): CourtCaseResponse = CourtCaseResponse(
  id = this.id,
  offenderNo = this.offenderBooking.offender.nomsId,
  bookingId = this.offenderBooking.bookingId,
  primaryCaseInfoNumber = this.primaryCaseInfoNumber,
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
  caseInfoNumbers = this.caseInfoNumbers.filter { it.isDpsCaseInfoNumber() }.map { it.toCaseIdentifier() },
)

private fun OffenderCaseIdentifier.toCaseIdentifier(): CaseIdentifierResponse = CaseIdentifierResponse(
  reference = this.id.reference,
  type = this.id.identifierType,
  createDateTime = this.createDatetime,
  auditModuleName = this.auditModuleName,
  modifiedDateTime = this.modifyDatetime,
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
