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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LinkCaseTxn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LinkCaseTxnId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason.Companion.RECALL_BREACH_HEARING
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenceResultCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenceResultCode.Companion.RECALL_TO_PRISON
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseIdentifierPK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderFixedTermRecall
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentencePurposeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceTermType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventChargeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtOrderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.LinkCaseTxnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.MergeTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceResultCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderChargeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderFixedTermRecallRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceTermRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SentenceCalculationTypeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.ImprisonmentStatusChangeType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional
class SentencingService(
  private val courtCaseRepository: CourtCaseRepository,
  private val offenderRepository: OffenderRepository,
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
  private val mergeTransactionRepository: MergeTransactionRepository,
  private val staffUserAccountRepository: StaffUserAccountRepository,
  private val offenderFixedTermRecallRepository: OffenderFixedTermRecallRepository,
  private val sentencingAdjustmentService: SentencingAdjustmentService,
  private val linkCaseTxnRepository: LinkCaseTxnRepository,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val ACTIVE_CHARGE_STATUS = "A"
    private const val PARTIAL_RESULT_CODE_INDICATOR = "P"
    private const val FINAL_RESULT_CODE_INDICATOR = "F"
  }

  fun getCourtCase(id: Long, offenderNo: String): CourtCaseResponse {
    checkOffenderExists(offenderNo)
    return courtCaseRepository.findByIdOrNull(id)?.toCourtCaseResponse()
      ?: throw NotFoundException("Court case $id not found")
  }

  fun getCourtCaseForMigration(id: Long): CourtCaseResponse = courtCaseRepository.findByIdOrNull(id)?.toCourtCaseResponse()
    ?: throw NotFoundException("Court case $id not found")

  fun getCourtCasesByOffender(offenderNo: String): List<CourtCaseResponse> = courtCaseRepository.findByOffenderBookingOffenderNomsIdOrderByCreateDatetimeDesc(offenderNo)
    .map { courtCase ->
      courtCase.toCourtCaseResponse()
    }

  fun getCourtCasesChangedByMergePrisoners(offenderNo: String): PostPrisonerMergeCaseChanges {
    val lastMerge = mergeTransactionRepository.findLatestByNomsId(offenderNo)
      ?: throw BadDataException("Prisoner $offenderNo has no merges")
    val allCases = courtCaseRepository.findByOffenderBookingOffenderNomsIdOrderByCreateDatetimeDesc(offenderNo)
    val casesDeactivatedByMerge = allCases.filter { it.wasDeactivatedByMerge(lastMerge.requestDate) }
    // if no cases were deactivated by merge then none would have been cloned
    if (casesDeactivatedByMerge.isNotEmpty()) {
      val casesCreated = allCases.filter { it.wasCreatedByMerge(lastMerge.requestDate) }
      return PostPrisonerMergeCaseChanges(
        courtCasesCreated = casesCreated.map { it.toCourtCaseResponse() },
        courtCasesDeactivated = casesDeactivatedByMerge.map { it.toCourtCaseResponse() },
      )
    }
    return PostPrisonerMergeCaseChanges()
  }

  private fun CourtCase.wasDeactivatedByMerge(mergeRequestDate: LocalDateTime) = caseStatus.code == CaseStatus.INACTIVE && auditModuleName == "MERGE" && modifyDatetime != null && mergeRequestDate < modifyDatetime

  private fun CourtCase.wasCreatedByMerge(mergeRequestDate: LocalDateTime) = mergeRequestDate < createDatetime && createUsername == "SYS"

  fun getOffenderCharge(id: Long): OffenderCharge = offenderChargeRepository.findByIdOrNull(id)
    ?: throw NotFoundException("Offender Charge $id not found")

  @Audit
  fun createCourtCase(offenderNo: String, request: CreateCourtCaseRequest) = findLatestBooking(offenderNo).let { booking ->

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

  fun findCourtCaseIdsByFilter(pageRequest: Pageable, courtCaseFilter: CourtCaseFilter): Page<CourtCaseIdResponse> = if (courtCaseFilter.toDateTime == null && courtCaseFilter.fromDateTime == null) {
    courtCaseRepository.findAllCourtCaseIds(pageable = pageRequest).map { CourtCaseIdResponse(it) }
  } else {
    courtCaseRepository.findAllCourtCaseIds(
      fromDateTime = courtCaseFilter.fromDateTime,
      toDateTime = courtCaseFilter.toDateTime,
      pageable = pageRequest,
    ).map { CourtCaseIdResponse(it) }
  }

  fun findCourtCaseIdsByOffender(offenderNo: String): List<Long> = courtCaseRepository.findCourtCaseIdsForOffender(
    offenderNo = offenderNo,
  )

  // updates to charges are triggered by a separate endpoint
  @Audit
  fun createCourtAppearance(
    offenderNo: String,
    caseId: Long,
    request: CourtAppearanceRequest,
  ): CreateCourtAppearanceResponse {
    checkOffenderExists(offenderNo)
    findCourtCase(caseId, offenderNo).let {
      val (courtCase, courtAppearanceRequest, clonedCourtCases) = cloneCasesIfRequired(it, request)

      val courtEvent = CourtEvent(
        offenderBooking = courtCase.offenderBooking,
        courtCase = courtCase,
        eventDate = courtAppearanceRequest.eventDateTime.toLocalDate(),
        startTime = courtAppearanceRequest.eventDateTime,
        courtEventType = lookupMovementReasonType(courtAppearanceRequest.courtEventType),
        eventStatus = determineEventStatus(
          courtAppearanceRequest.eventDateTime.toLocalDate(),
          courtCase.offenderBooking,
        ),
        court = lookupEstablishment(courtAppearanceRequest.courtId),
        outcomeReasonCode = courtAppearanceRequest.outcomeReasonCode?.let { lookupOffenceResultCode(it) },
        nextEventDate = courtAppearanceRequest.nextEventDateTime?.toLocalDate(),
        nextEventStartTime = courtAppearanceRequest.nextEventDateTime,
        directionCode = lookupDirectionType(DirectionType.OUT),
      )
      // 'to update' in this context of a new appearance means that the offender charges exist and are associated
      associateChargesWithAppearance(courtAppearanceRequest.courtEventCharges, courtEvent)

      courtCase.courtEvents.add(
        courtEvent,
      )
      courtEventRepository.saveAndFlush(courtEvent).let { createdCourtEvent ->
        refreshCourtOrder(courtEvent = createdCourtEvent, offenderNo = offenderNo)
        storedProcedureRepository.imprisonmentStatusUpdate(
          bookingId = courtCase.offenderBooking.bookingId,
          changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
        )
        return CreateCourtAppearanceResponse(
          id = createdCourtEvent.id,
          clonedCourtCases = clonedCourtCases,
        ).also { response ->
          telemetryClient.trackEvent(
            "court-appearance-created",
            mapOf(
              "courtCaseId" to caseId.toString(),
              "clonedCourtCaseId" to courtCase.id.toString(),
              "courtCaseCloned" to (caseId != courtCase.id).toString(),
              "bookingId" to courtCase.offenderBooking.bookingId.toString(),
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

  data class ClonedCaseCreateAppearance(val courtCase: CourtCase, val courtAppearanceRequest: CourtAppearanceRequest, val clonedCourtCases: BookingCourtCaseCloneResponse?)
  private fun cloneCasesIfRequired(courtCase: CourtCase, courtAppearanceRequest: CourtAppearanceRequest): ClonedCaseCreateAppearance = if (courtCase.offenderBooking.bookingSequence == 1) {
    ClonedCaseCreateAppearance(
      courtCase = courtCase,
      courtAppearanceRequest = courtAppearanceRequest,
      clonedCourtCases = null,
    )
  } else {
    cloneCourtCasesToLatestBookingFrom(courtCase).let {
      val sourceCase = it.courtCases.find { cases -> cases.sourceCourtCase.id == courtCase.id }!!
      val indexOfSourceCase = it.courtCases.indexOf(sourceCase)
      val clonedCourtCase = findCourtCase(id = it.courtCases[indexOfSourceCase].courtCase.id, sourceCase.sourceCourtCase.offenderNo)
      val offenderChargesIndexes = courtAppearanceRequest.courtEventCharges.map { courtEventChargeId -> courtCase.offenderCharges.indexOf(courtCase.offenderCharges.find { offenderCharge -> offenderCharge.id == courtEventChargeId }) }
      val convertedOffenderChargeIds = offenderChargesIndexes.map { index -> clonedCourtCase.offenderCharges[index].id }
      ClonedCaseCreateAppearance(
        // this will switch the case that has been created by the clone if it was cloned
        courtCase = clonedCourtCase,
        // offender charges in request now point at the newly created ones
        courtAppearanceRequest = courtAppearanceRequest.copy(
          courtEventCharges = convertedOffenderChargeIds,
        ),
        clonedCourtCases = it,
      )
    }
  }

  // creates offender charge without associating with a Court Event
  fun createCourtCharge(
    offenderNo: String,
    caseId: Long,
    offenderChargeRequest: OffenderChargeRequest,
  ): OffenderChargeIdResponse {
    checkOffenderExists(offenderNo)
    findCourtCase(caseId, offenderNo).let { courtCase ->
      val resultCode =
        offenderChargeRequest.resultCode1?.let { lookupOffenceResultCode(it) }
      val offenderCharge = OffenderCharge(
        courtCase = courtCase,
        offenderBooking = courtCase.offenderBooking,
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
            bookingId = courtCase.offenderBooking.bookingId,
            changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
          )
          telemetryClient.trackEvent(
            "offender-charge-created",
            mapOf(
              "courtCaseId" to caseId.toString(),
              "bookingId" to courtCase.offenderBooking.bookingId.toString(),
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

  fun getCourtAppearance(id: Long, offenderNo: String): CourtEventResponse {
    checkOffenderExists(offenderNo)
    return findCourtAppearance(offenderNo = offenderNo, id = id).toCourtEvent()
  }

  // associate (or remove) charges with appearance if not already associated
  private fun associateChargesWithAppearance(
    courtEventChargesToUpdate: List<Long>,
    courtEvent: CourtEvent,
    useCourtEventOutcome: Boolean = false,
  ) {
    val originalList = courtEvent.courtEventCharges
    val newChargeList = mutableListOf<CourtEventCharge>()
    courtEventChargesToUpdate.map { requestChargeId ->
      courtEvent.courtEventCharges.firstOrNull { it.id.offenderCharge.id == requestChargeId }
        ?.let { existingCourtEventCharge ->
          newChargeList.add(existingCourtEventCharge)
        } ?: let {
        getOffenderCharge(requestChargeId).let { offenderCharge ->
          val resultCode = if (useCourtEventOutcome) {
            courtEvent.outcomeReasonCode
          } else {
            offenderCharge.resultCode1 ?: courtEvent.outcomeReasonCode
          }
          newChargeList.add(
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
    log.info("Court event charges for appearance ${courtEvent.id}\noriginalList: $originalList\nnewList: $newChargeList")
    courtEvent.courtEventCharges.clear()
    courtEvent.courtEventCharges.addAll(newChargeList)
  }

  @Audit
  fun updateCourtAppearance(
    offenderNo: String,
    caseId: Long,
    eventId: Long,
    request: CourtAppearanceRequest,
  ): UpdateCourtAppearanceResponse {
    checkOffenderExists(offenderNo)
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

        associateChargesWithAppearance(request.courtEventCharges, courtAppearance)

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
            bookingId = courtCase.offenderBooking.bookingId,
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

  @Audit
  fun deleteCourtAppearance(
    offenderNo: String,
    caseId: Long,
    eventId: Long,
  ) {
    findCourtCase(caseId, offenderNo).let { case ->
      val telemetry = mapOf(
        "bookingId" to case.offenderBooking.bookingId.toString(),
        "offenderNo" to offenderNo,
        "eventId" to eventId.toString(),
        "caseId" to caseId.toString(),
      )
      courtEventRepository.findByIdOrNull(eventId)?.also {
        courtEventRepository.delete(it)
        storedProcedureRepository.imprisonmentStatusUpdate(
          bookingId = case.offenderBooking.bookingId,
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
    var telemetry = mapOf(
      "offenderNo" to offenderNo,
      "caseId" to caseId.toString(),
    )
    courtCaseRepository.findByIdOrNull(caseId)?.also {
      telemetry = telemetry + ("bookingId" to it.offenderBooking.bookingId.toString())
      courtCaseRepository.delete(it)
      storedProcedureRepository.imprisonmentStatusUpdate(
        bookingId = it.offenderBooking.bookingId,
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

  @Audit
  fun updateCourtCharge(
    offenderNo: String,
    caseId: Long,
    chargeId: Long,
    courtEventId: Long,
    request: OffenderChargeRequest,
  ) {
    checkOffenderExists(offenderNo)
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
              bookingId = courtCase.offenderBooking.bookingId,
              changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
            )
            telemetryClient.trackEvent(
              "court-charge-updated",
              mapOf(
                "courtCaseId" to caseId.toString(),
                "bookingId" to courtCase.offenderBooking.bookingId.toString(),
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

  @Audit
  fun createSentence(offenderNo: String, caseId: Long, request: CreateSentenceRequest) = findCourtCaseWithLock(id = caseId, offenderNo = offenderNo).let { case ->

    val offenderBooking = case.offenderBooking
    checkConsecutiveSentenceExists(request, offenderBooking)
    val newSequence = offenderSentenceRepository.getNextSequence(offenderBooking)
    val sentence = OffenderSentence(
      id = SentenceId(offenderBooking, sequence = newSequence),
      category = lookupSentenceCategory(request.sentenceCategory),
      calculationType = lookupSentenceCalculationType(
        categoryCode = request.sentenceCategory,
        calcType = request.sentenceCalcType,
      ),
      courtCase = case,
      startDate = request.startDate,
      endDate = request.endDate,
      status = request.status,
      fineAmount = request.fine,
      sentenceLevel = request.sentenceLevel,
      // will always have to be a court order (via the court event) - throw exception if not
      courtOrder = existingCourtOrder(
        offenderBooking = offenderBooking,
        courtEvent = findCourtAppearance(offenderNo = offenderNo, id = request.eventId),
      )
        ?: throw BadDataException("Court order not found for booking ${offenderBooking.bookingId} and court event ${request.eventId}"),
      // this is the sentence sequence this sentence is consecutive to
      consecSequence = request.consecutiveToSentenceSeq?.toInt(),
      lineSequence = offenderSentenceRepository.getNextLineSequence(offenderBooking).toInt(),
    )

    sentence.offenderSentenceCharges.addAll(
      request.offenderChargeIds.map { chargeId ->
        OffenderSentenceCharge(
          id = OffenderSentenceChargeId(
            offenderBooking = offenderBooking,
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
        bookingId = offenderBooking.bookingId,
        changeType = ImprisonmentStatusChangeType.UPDATE_SENTENCE.name,
      )
    }

    CreateSentenceResponse(
      sentenceSeq = sentence.id.sequence,
      bookingId = offenderBooking.bookingId,
    ).also { response ->
      telemetryClient.trackEvent(
        "sentence-created",
        mapOf(
          "sentenceSeq" to response.sentenceSeq.toString(),
          "bookingId" to offenderBooking.bookingId.toString(),
          "offenderNo" to offenderNo,
        ),
        null,
      )
    }
  }

  @Audit
  fun createSentenceTerm(offenderNo: String, caseId: Long, sentenceSequence: Long, termRequest: SentenceTermRequest) = findCourtCaseWithLock(id = caseId, offenderNo = offenderNo).let { case ->

    val offenderBooking = case.offenderBooking
    val termSequence = offenderSentenceTermRepository.getNextTermSequence(
      offenderBookId = offenderBooking.bookingId,
      sentenceSeq = sentenceSequence,
    )
    val sentence = findSentence(sentenceSequence = sentenceSequence, booking = offenderBooking)
    val term = OffenderSentenceTerm(
      id = OffenderSentenceTermId(
        offenderBooking = offenderBooking,
        sentenceSequence = sentenceSequence,
        termSequence = termSequence,
      ),
      years = termRequest.years,
      months = termRequest.months,
      weeks = termRequest.weeks,
      days = termRequest.days,
      hours = termRequest.hours,
      lifeSentenceFlag = termRequest.lifeSentenceFlag,
      offenderSentence = sentence,
      // DPS have requested that the court date from Court orders is used here, always present
      startDate = sentence.courtOrder!!.courtDate,
      endDate = null,
      sentenceTermType = lookupSentenceTermType(termRequest.sentenceTermType),
    )
    offenderSentenceTermRepository.saveAndFlush(term).also {
      storedProcedureRepository.imprisonmentStatusUpdate(
        bookingId = offenderBooking.bookingId,
        changeType = ImprisonmentStatusChangeType.UPDATE_SENTENCE.name,
      )
    }

    CreateSentenceTermResponse(
      sentenceSeq = sentenceSequence,
      termSeq = term.id.termSequence,
      bookingId = offenderBooking.bookingId,
    ).also { response ->
      telemetryClient.trackEvent(
        "sentence-term-created",
        mapOf(
          "sentenceSeq" to response.sentenceSeq.toString(),
          "termSeq" to response.termSeq.toString(),
          "bookingId" to offenderBooking.bookingId.toString(),
          "offenderNo" to offenderNo,
        ),
        null,
      )
    }
  }

  @Audit
  fun deleteSentence(offenderNo: String, caseId: Long, sentenceSequence: Long) {
    findCourtCase(id = caseId, offenderNo = offenderNo).let { case ->
      val offenderBooking = case.offenderBooking
      offenderSentenceRepository.findByIdOrNull(
        SentenceId(
          offenderBooking = offenderBooking,
          sequence = sentenceSequence,
        ),
      )?.also {
        offenderSentenceRepository.delete(it)
        telemetryClient.trackEvent(
          "sentence-deleted",
          mapOf(
            "bookingId" to offenderBooking.bookingId.toString(),
            "offenderNo" to offenderNo,
            "sentenceSequence" to it.id.sequence.toString(),
          ),
          null,
        )
      }
        ?: telemetryClient.trackEvent(
          "sentence-delete-not-found",
          mapOf(
            "bookingId" to offenderBooking.bookingId.toString(),
            "sentenceSequence" to sentenceSequence.toString(),
          ),
          null,
        )
    }
  }

  @Audit
  fun deleteSentenceTerm(offenderNo: String, caseId: Long, sentenceSequence: Long, termSequence: Long) {
    findCourtCase(id = caseId, offenderNo = offenderNo).let { case ->
      val offenderBooking = case.offenderBooking
      offenderSentenceTermRepository.findByIdOrNull(
        OffenderSentenceTermId(
          offenderBooking = offenderBooking,
          sentenceSequence = sentenceSequence,
          termSequence = termSequence,
        ),
      )?.also {
        offenderSentenceTermRepository.delete(it)
        telemetryClient.trackEvent(
          "sentence-term-deleted",
          mapOf(
            "bookingId" to offenderBooking.bookingId.toString(),
            "offenderNo" to offenderNo,
            "sentenceSequence" to sentenceSequence.toString(),
            "termSequence" to termSequence.toString(),
          ),
          null,
        )
      }
        ?: telemetryClient.trackEvent(
          "sentence-term-delete-not-found",
          mapOf(
            "bookingId" to offenderBooking.bookingId.toString(),
            "sentenceSequence" to sentenceSequence.toString(),
            "termSequence" to termSequence.toString(),
          ),
          null,
        )
    }
  }

  @Audit
  fun updateSentence(
    caseId: Long,
    sentenceSequence: Long,
    request: CreateSentenceRequest,
    offenderNo: String,
  ) {
    findCourtCase(id = caseId, offenderNo = offenderNo).let { case ->
      findSentence(booking = case.offenderBooking, sentenceSequence = sentenceSequence).let { sentence ->
        val offenderBooking = case.offenderBooking
        checkConsecutiveSentenceExists(request, offenderBooking)
        sentence.category = lookupSentenceCategory(request.sentenceCategory)
        sentence.calculationType = lookupSentenceCalculationType(
          categoryCode = request.sentenceCategory,
          calcType = request.sentenceCalcType,
        )
        sentence.courtCase =
          findCourtCase(id = caseId, offenderNo = case.offenderBooking.offender.nomsId)
        sentence.startDate = request.startDate
        sentence.endDate = request.endDate
        sentence.status = request.status
        sentence.fineAmount = request.fine
        sentence.sentenceLevel = request.sentenceLevel
        sentence.consecSequence = request.consecutiveToSentenceSeq?.toInt()
        sentence.courtOrder = existingCourtOrder(
          offenderBooking = offenderBooking,
          courtEvent = findCourtAppearance(offenderNo = offenderNo, id = request.eventId),
        )
          ?: throw BadDataException("Court order not found for booking ${offenderBooking.bookingId} and court event ${request.eventId}")

        log.info(
          "\nUpdating sentence charges for sentence $sentenceSequence, booking ${case.offenderBooking.bookingId} and offender $offenderNo " +
            "\nwith charges: ${request.offenderChargeIds} " +
            "\noriginal charges: ${sentence.offenderSentenceCharges.map { it.offenderCharge.id }}",
        )

        if (sentence.offenderSentenceCharges.map { it.offenderCharge.id }
            .toSet() != request.offenderChargeIds.toSet()
        ) {
          sentence.offenderSentenceCharges.clear()
          sentence.offenderSentenceCharges.addAll(
            request.offenderChargeIds.map { chargeId ->
              OffenderSentenceCharge(
                id = OffenderSentenceChargeId(
                  offenderBooking = case.offenderBooking,
                  sequence = sentence.id.sequence,
                  offenderChargeId = chargeId,
                ),
                offenderSentence = sentence,
                offenderCharge = findOffenderCharge(offenderNo = offenderNo, id = chargeId),
              )
            },
          )
        }

        offenderSentenceRepository.saveAndFlush(sentence).also {
          storedProcedureRepository.imprisonmentStatusUpdate(
            bookingId = case.offenderBooking.bookingId,
            changeType = ImprisonmentStatusChangeType.UPDATE_SENTENCE.name,
          )
        }

        telemetryClient.trackEvent(
          "sentence-updated",
          mapOf(
            "bookingId" to case.offenderBooking.bookingId.toString(),
            "sentenceSequence" to sentenceSequence.toString(),
            "caseId" to caseId.toString(),
            "offenderNo" to offenderNo,
            "charges" to request.offenderChargeIds.toString(),
          ),
          null,
        )
      }
    }
  }

  private fun checkConsecutiveSentenceExists(
    request: CreateSentenceRequest,
    offenderBooking: OffenderBooking,
  ) {
    request.consecutiveToSentenceSeq?.let {
      offenderSentenceRepository.findByIdOrNull(
        SentenceId(
          sequence = it,
          offenderBooking = offenderBooking,
        ),
      )
        ?: throw NotFoundException("Consecutive sentence with sequence ${request.consecutiveToSentenceSeq} and booking ${offenderBooking.bookingId} not found")
    }
  }

  @Audit
  fun updateSentenceTerm(
    caseId: Long,
    sentenceSequence: Long,
    termSequence: Long,
    termRequest: SentenceTermRequest,
    offenderNo: String,
  ) {
    findCourtCase(id = caseId, offenderNo = offenderNo).let { case ->
      findSentenceTerm(
        booking = case.offenderBooking,
        sentenceSequence = sentenceSequence,
        termSequence = termSequence,
        offenderNo = offenderNo,
      ).let { term ->

        term.years = termRequest.years
        term.months = termRequest.months
        term.weeks = termRequest.weeks
        term.days = termRequest.days
        term.hours = termRequest.hours
        term.lifeSentenceFlag = termRequest.lifeSentenceFlag
        term.sentenceTermType = lookupSentenceTermType(termRequest.sentenceTermType)

        offenderSentenceTermRepository.saveAndFlush(term).also {
          storedProcedureRepository.imprisonmentStatusUpdate(
            bookingId = case.offenderBooking.bookingId,
            changeType = ImprisonmentStatusChangeType.UPDATE_SENTENCE.name,
          )
        }

        telemetryClient.trackEvent(
          "sentence-term-updated",
          mapOf(
            "bookingId" to case.offenderBooking.bookingId.toString(),
            "sentenceSequence" to sentenceSequence.toString(),
            "caseId" to caseId.toString(),
            "offenderNo" to offenderNo,
            "termSequence" to termSequence.toString(),
          ),
          null,
        )
      }
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

  fun refreshCourtOrder(
    courtEvent: CourtEvent,
    offenderNo: String,
  ) {
    if (courtEvent.courtEventCharges.any { charge -> charge.resultCode1?.resultRequiresACourtOrder() == true }) {
      existingCourtOrder(courtEvent.offenderBooking, courtEvent)?.let { order ->
        // only amending orders without a sentence
        if (!courtEvent.courtEventCharges.map { it.id.offenderCharge }
            .any { it.offenderSentenceCharges.isNotEmpty() }
        ) {
          if (order.courtDate != courtEvent.eventDate) {
            order.courtDate = courtEvent.eventDate
            telemetryClient.trackEvent(
              "court-order-updated",
              mapOf(
                "courtCaseId" to courtEvent.courtCase!!.id.toString(),
                "bookingId" to courtEvent.offenderBooking.bookingId.toString(),
                "offenderNo" to offenderNo,
                "court" to courtEvent.court.id,
                "courtEventId" to courtEvent.id.toString(),
                "orderId" to order.id.toString(),
                "orderDate" to courtEvent.eventDate.toString(),
              ),
              null,
            )
          }
        }
      } ?: let {
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

  fun OffenceResultCode.resultRequiresACourtOrder(): Boolean = this.chargeStatus == ACTIVE_CHARGE_STATUS && (this.dispositionCode == PARTIAL_RESULT_CODE_INDICATOR || this.dispositionCode == FINAL_RESULT_CODE_INDICATOR)

  private fun existingCourtOrder(offenderBooking: OffenderBooking, courtEvent: CourtEvent) = courtOrderRepository.findByOffenderBookingAndCourtEventAndOrderType(offenderBooking, courtEvent)

  fun determineEventStatus(eventDate: LocalDate, booking: OffenderBooking): EventStatus = if (eventDate < booking.bookingBeginDate.toLocalDate()
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

  fun getOffenderSentence(offenderNo: String, caseId: Long, sentenceSequence: Long): SentenceResponse = findCourtCase(id = caseId, offenderNo = offenderNo).let { case ->
    val offenderBooking = case.offenderBooking

    return offenderSentenceRepository.findByIdOrNull(
      SentenceId(
        offenderBooking = offenderBooking,
        sequence = sentenceSequence,
      ),
    )?.toSentenceResponse()
      ?: throw NotFoundException("Offender sentence for booking ${offenderBooking.bookingId} and sentence sequence $sentenceSequence not found")
  }

  fun getOffenderSentenceTerm(
    offenderNo: String,
    offenderBookingId: Long,
    termSequence: Long,
    sentenceSequence: Long,
  ): SentenceTermResponse = findOffenderBooking(id = offenderBookingId).let { offenderBooking ->
    return findSentenceTerm(
      offenderNo = offenderNo,
      booking = offenderBooking,
      sentenceSequence = sentenceSequence,
      termSequence = termSequence,
    ).toSentenceTermResponse()
  }

  fun getActiveRecallSentencesByBookingId(bookingId: Long): List<SentenceResponse> {
    val offenderBooking = findOffenderBooking(id = bookingId)
    return offenderBooking.sentences
      .filter { it.isActiveRecallSentence() }
      .map { it.toSentenceResponse() }
  }

  fun getOffenderCharge(id: Long, offenderNo: String): OffenderChargeResponse {
    checkOffenderExists(offenderNo)
    return findOffenderCharge(offenderNo = offenderNo, id = id).toOffenderCharge()
  }

  fun getCourtEventCharge(chargeId: Long, eventId: Long, offenderNo: String): CourtEventChargeResponse {
    val charge = getOffenderCharge(chargeId)
    val appearance = findCourtAppearance(offenderNo = offenderNo, id = eventId)
    return findCourtEventCharge(
      CourtEventChargeId(offenderCharge = charge, courtEvent = appearance),
      offenderNo = offenderNo,
    ).toCourtEventCharge()
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

  fun convertToRecallSentences(offenderNo: String, request: ConvertToRecallRequest): ConvertToRecallResponse {
    // It would be odd for the sentences to sit across bookings but give DPS is booking agnostic,
    // it would make sense to not make any assumptions
    val bookingIds = request.sentences.map { it.sentenceId.offenderBookingId }.toSet()

    val sentencesUpdated = request.sentences.updateSentences()
    sentencingAdjustmentService.convertAdjustmentsToRecallEquivalents(sentencesUpdated)
    val adjustmentsUpdated = sentencingAdjustmentService.activateAllAdjustment(sentencesUpdated)
    request.returnToCustody.createOrUpdateBooking(bookingIds)

    // Create a new CourtEvent for each unique CourtCase associated with each OffenderSentence
    val uniqueCourtCases = sentencesUpdated.mapNotNull { it.courtCase }.toSet()
    // Create a new CourtEvent for each unique CourtCase
    val courtEvents = uniqueCourtCases.map { courtCase ->
      // Find all sentences associated with this court case that are being recalled
      val sentencesForCase = sentencesUpdated.filter {
        it.courtCase == courtCase
      }

      // Find the court from the last appearance
      val court = courtCase.courtEvents.maxByOrNull(CourtEvent::eventDate)!!.court

      // Create the court event
      val courtEvent = CourtEvent(
        offenderBooking = courtCase.offenderBooking,
        courtCase = courtCase,
        eventDate = request.recallRevocationDate,
        startTime = LocalDateTime.of(request.recallRevocationDate, LocalTime.MIDNIGHT),
        courtEventType = lookupMovementReasonType(RECALL_BREACH_HEARING),
        eventStatus = determineEventStatus(
          request.recallRevocationDate,
          courtCase.offenderBooking,
        ),
        court = court,
        outcomeReasonCode = lookupOffenceResultCode(RECALL_TO_PRISON),
        directionCode = lookupDirectionType(DirectionType.OUT),
      )

      // Create CourtEventCharge for each offenderSentenceCharge in the OffenderSentence
      val offenderChargeIds =
        sentencesForCase.flatMap { sentence -> sentence.offenderSentenceCharges.map { it.offenderCharge.id } }.toSet()
      // Associate charges with the court event
      associateChargesWithAppearance(
        courtEventChargesToUpdate = offenderChargeIds.toList(),
        courtEvent = courtEvent,
        useCourtEventOutcome = true,
      )

      courtEventRepository.saveAndFlush(courtEvent)
    }

    bookingIds.forEach { bookingId ->
      storedProcedureRepository.imprisonmentStatusUpdate(
        bookingId = bookingId,
        changeType = ImprisonmentStatusChangeType.UPDATE_SENTENCE.name,
      )
    }
    telemetryClient.trackEvent(
      "sentences-recalled",
      mapOf(
        "bookingId" to bookingIds.joinToString { it.toString() },
        "sentenceSequences" to request.sentences.map { it.sentenceId.sentenceSequence }.joinToString { it.toString() },
        "offenderNo" to offenderNo,
      ),
      null,
    )

    return ConvertToRecallResponse(
      courtEventIds = courtEvents.map { it.id },
      sentenceAdjustmentsActivated = adjustmentsUpdated.filter { it.adjustmentIds.isNotEmpty() }.map {
        SentenceIdAndAdjustmentIds(
          sentenceId = SentenceId(
            offenderBookingId = it.sentenceId.offenderBooking.bookingId,
            sentenceSequence = it.sentenceId.sequence,
          ),
          adjustmentIds = it.adjustmentIds,
        )
      },
    )
  }

  fun updateRecallSentences(offenderNo: String, request: UpdateRecallRequest) {
    val bookingIds = request.sentences.map { it.sentenceId.offenderBookingId }.toSet()

    request.sentences.updateSentences()
    request.returnToCustody.createOrUpdateBooking(bookingIds)
    bookingIds.forEach { bookingId ->
      storedProcedureRepository.imprisonmentStatusUpdate(
        bookingId = bookingId,
        changeType = ImprisonmentStatusChangeType.UPDATE_SENTENCE.name,
      )
    }
    request.beachCourtEventIds.forEach {
      courtEventRepository.findByIdOrNull(it)?.also { courtEvent ->
        courtEvent.eventDate = request.recallRevocationDate
        courtEvent.startTime = LocalDateTime.of(request.recallRevocationDate, LocalTime.MIDNIGHT)
      }
    }
    telemetryClient.trackEvent(
      "recall-sentences-updated",
      mapOf(
        "bookingId" to bookingIds.joinToString { it.toString() },
        "sentenceSequences" to request.sentences.map { it.sentenceId.sentenceSequence }.joinToString { it.toString() },
        "offenderNo" to offenderNo,
        "beachCourtEventIds" to request.beachCourtEventIds.joinToString { it.toString() },
      ),
      null,
    )
  }

  fun revertRecallSentences(offenderNo: String, request: RevertRecallRequest) {
    val bookingIds = request.sentences.map { it.sentenceId.offenderBookingId }.toSet()

    request.sentences.updateSentences()
    request.returnToCustody.createOrUpdateBooking(bookingIds)
    bookingIds.forEach { bookingId ->
      storedProcedureRepository.imprisonmentStatusUpdate(
        bookingId = bookingId,
        changeType = ImprisonmentStatusChangeType.UPDATE_SENTENCE.name,
      )
    }

    courtEventRepository.deleteAllById(request.beachCourtEventIds)

    telemetryClient.trackEvent(
      "recall-sentences-reverted",
      mapOf(
        "bookingId" to bookingIds.joinToString { it.toString() },
        "sentenceSequences" to request.sentences.map { it.sentenceId.sentenceSequence }.joinToString { it.toString() },
        "offenderNo" to offenderNo,
        "beachCourtEventIds" to request.beachCourtEventIds.joinToString { it.toString() },
      ),
      null,
    )
  }

  fun replaceRecallSentences(offenderNo: String, request: DeleteRecallRequest) {
    val bookingIds = request.sentences.map { it.sentenceId.offenderBookingId }.toSet()

    val sentencesUpdated = request.sentences.updateSentences()
    sentencingAdjustmentService.convertAdjustmentsToPreRecallEquivalents(sentencesUpdated)
    bookingIds.forEach { bookingId ->
      findOffenderBooking(bookingId).fixedTermRecall = null
      offenderFixedTermRecallRepository.deleteById(bookingId)
      storedProcedureRepository.imprisonmentStatusUpdate(
        bookingId = bookingId,
        changeType = ImprisonmentStatusChangeType.UPDATE_SENTENCE.name,
      )
    }
    courtEventRepository.deleteAllById(request.beachCourtEventIds)
    telemetryClient.trackEvent(
      "recall-sentences-replaced",
      mapOf(
        "bookingId" to bookingIds.joinToString { it.toString() },
        "sentenceSequences" to request.sentences.map { it.sentenceId.sentenceSequence }.joinToString { it.toString() },
        "offenderNo" to offenderNo,
        "beachCourtEventIds" to request.beachCourtEventIds.joinToString { it.toString() },
      ),
      null,
    )
  }

  private fun ReturnToCustodyRequest?.createOrUpdateBooking(bookingIds: Set<Long>) {
    if (this != null) {
      val enteredByStaff = findStaffByUsername(this.enteredByStaffUsername)
      bookingIds.forEach { bookingId ->
        with(findOffenderBooking(bookingId)) {
          if (fixedTermRecall == null) {
            fixedTermRecall = OffenderFixedTermRecall(
              returnToCustodyDate = this@createOrUpdateBooking.returnToCustodyDate,
              staff = enteredByStaff,
              recallLength = this@createOrUpdateBooking.recallLength.toLong(),
              offenderBooking = this,
            )
          } else {
            with(fixedTermRecall!!) {
              returnToCustodyDate = this@createOrUpdateBooking.returnToCustodyDate
              staff = enteredByStaff
              recallLength = this@createOrUpdateBooking.recallLength.toLong()
            }
          }
        }
      }
    } else {
      bookingIds.forEach { bookingId ->
        findOffenderBooking(bookingId).fixedTermRecall = null
        offenderFixedTermRecallRepository.deleteById(bookingId)
      }
    }
  }

  private fun List<RecallRelatedSentenceDetails>.updateSentences(): List<OffenderSentence> = this.map { sentence ->
    val offenderBooking = findOffenderBooking(sentence.sentenceId.offenderBookingId)
    with(findSentence(booking = offenderBooking, sentenceSequence = sentence.sentenceId.sentenceSequence)) {
      category = lookupSentenceCategory(sentence.sentenceCategory)
      calculationType = lookupSentenceCalculationType(
        categoryCode = sentence.sentenceCategory,
        calcType = sentence.sentenceCalcType,
      )
      status = if (sentence.active) "A" else "I"
      offenderSentenceRepository.saveAndFlush(this)
    }
  }

  private fun findLatestBooking(offenderNo: String): OffenderBooking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
    ?: throw NotFoundException("Prisoner $offenderNo not found or has no bookings")

  private fun checkOffenderExists(offenderNo: String): Boolean {
    if (offenderRepository.existsByNomsId(offenderNo)) {
      return true
    }
    throw NotFoundException("Prisoner $offenderNo not found")
  }

  private fun findOffenderBooking(id: Long): OffenderBooking = offenderBookingRepository.findByIdOrNull(id)
    ?: throw NotFoundException("Offender booking $id not found")

  private fun findCourtCase(id: Long, offenderNo: String): CourtCase = courtCaseRepository.findByIdOrNull(id)
    ?: throw NotFoundException("Court case $id for $offenderNo not found")

  private fun findCourtCaseWithLock(id: Long, offenderNo: String): CourtCase = courtCaseRepository.findByIdOrNullForUpdate(id)
    ?: throw NotFoundException("Court case $id for $offenderNo not found")

  private fun findCourtAppearance(id: Long, offenderNo: String): CourtEvent = courtEventRepository.findByIdOrNull(id)
    ?: throw NotFoundException("Court appearance $id for $offenderNo not found")

  private fun findSentence(sentenceSequence: Long, booking: OffenderBooking): OffenderSentence = offenderSentenceRepository.findByIdOrNull(SentenceId(sequence = sentenceSequence, offenderBooking = booking))
    ?: throw NotFoundException("Sentence for booking ${booking.bookingId} and sentence sequence $sentenceSequence not found")

  private fun findSentenceTerm(
    termSequence: Long,
    sentenceSequence: Long,
    booking: OffenderBooking,
    offenderNo: String,
  ): OffenderSentenceTerm = offenderSentenceTermRepository.findByIdOrNull(
    OffenderSentenceTermId(
      termSequence = termSequence,
      sentenceSequence = sentenceSequence,
      offenderBooking = booking,
    ),
  )
    ?: throw NotFoundException("Sentence term for offender $offenderNo, booking ${booking.bookingId}, term sequence $termSequence and sentence sequence $sentenceSequence not found")

  private fun findOffenderCharge(id: Long, offenderNo: String): OffenderCharge = offenderChargeRepository.findByIdOrNull(id)
    ?: throw NotFoundException("Offender Charge $id for $offenderNo not found")

  private fun findCourtEventCharge(id: CourtEventChargeId, offenderNo: String): CourtEventCharge = courtEventChargeRepository.findByIdOrNull(id)
    ?: throw NotFoundException("Court event charge with offenderChargeId ${id.offenderCharge.id} for $offenderNo not found")

  private fun lookupLegalCaseType(code: String): LegalCaseType = legalCaseTypeRepository.findByIdOrNull(
    LegalCaseType.pk(code),
  ) ?: throw BadDataException("Legal Case Type $code not found")

  private fun lookupSentenceTermType(code: String): SentenceTermType = sentenceTermTypeRepository.findByIdOrNull(
    SentenceTermType.pk(code),
  ) ?: throw BadDataException("Sentence term type $code not found")

  private fun lookupSentenceCategory(code: String): SentenceCategoryType = sentenceCategoryRepository.findByIdOrNull(
    SentenceCategoryType.pk(code),
  ) ?: throw BadDataException("Sentence category $code not found")

  private fun lookupSentenceCalculationType(categoryCode: String, calcType: String): SentenceCalculationType = sentenceCalculationTypeRepository.findByIdOrNull(
    SentenceCalculationTypeId(calculationType = calcType, category = categoryCode),
  )
    ?: throw BadDataException("Sentence calculation with category $categoryCode and calculation type $calcType not found")

  @Suppress("SameParameterValue")
  private fun lookupDirectionType(code: String): DirectionType = directionTypeRepository.findByIdOrNull(
    DirectionType.pk(code),
  ) ?: throw BadDataException("Case status $code not found")

  private fun lookupCaseStatus(code: String): CaseStatus = caseStatusRepository.findByIdOrNull(
    CaseStatus.pk(code),
  ) ?: throw BadDataException("Case status $code not found")

  private fun lookupEstablishment(courtId: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(courtId)
    ?: throw BadDataException("Establishment $courtId not found")

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

  private fun lookupOffenceResultCode(code: String): OffenceResultCode = offenceResultCodeRepository.findByIdOrNull(code)
    ?: throw BadDataException("Offence result code $code not found")

  private fun lookupChargeStatusType(code: String): ChargeStatusType = chargeStatusTypeRepository.findByIdOrNull(
    ChargeStatusType.pk(code),
  ) ?: throw BadDataException("Charge status Type $code not found")

  private fun findStaffByUsername(username: String): Staff = staffUserAccountRepository.findByUsername(username)?.staff
    ?: throw BadDataException("Username $username not found")

  fun cloneCourtCasesToLatestBookingFrom(bookingId: Long): BookingCourtCaseCloneResponse {
    val booking = offenderBookingRepository.findByIdOrNull(bookingId) ?: throw NotFoundException("Booking $bookingId not found")
    val latestBooking = findLatestBooking(booking.offender.nomsId)

    if (booking == latestBooking) {
      throw BadDataException("Cannot clone court cased from the latest booking $bookingId on to itself")
    }

    return cloneCourtCasesToLatestBookingFrom(latestBooking, booking.courtCases)
  }
  fun cloneCourtCasesToLatestBookingFrom(case: CourtCase): BookingCourtCaseCloneResponse {
    val booking = offenderBookingRepository.findByIdOrNull(case.offenderBooking.bookingId)!!
    val latestBooking = findLatestBooking(booking.offender.nomsId)

    if (booking == latestBooking) {
      throw BadDataException("Cannot clone court cased from the latest booking ${booking.bookingId} on to itself")
    }

    return cloneCourtCasesToLatestBookingFrom(latestBooking, booking.courtCases)
  }
  private fun cloneCourtCasesToLatestBookingFrom(latestBooking: OffenderBooking, sourceCourtCases: List<CourtCase>): BookingCourtCaseCloneResponse {
    val clonedCasesWithSource = sourceCourtCases.map { sourceCase ->
      courtCaseRepository.saveAndFlush(
        CourtCase(
          offenderBooking = latestBooking,
          legalCaseType = sourceCase.legalCaseType,
          beginDate = sourceCase.beginDate,
          // make case active unless this is a source of a linked case
          caseStatus = if (sourceCase.targetCombinedCase != null) sourceCase.caseStatus else lookupCaseStatus("A"),
          court = sourceCase.court,
          // set this later as the last update to offender case to prevent NOMIS trigger adding ti identifiers table
          primaryCaseInfoNumber = null,
          caseSequence = courtCaseRepository.getNextCaseSequence(latestBooking),
          caseInfoNumbers = mutableListOf(),
          statusUpdateDate = sourceCase.statusUpdateDate,
          statusUpdateStaff = sourceCase.statusUpdateStaff,
          statusUpdateComment = sourceCase.statusUpdateComment,
          statusUpdateReason = sourceCase.statusUpdateReason,

        ).also { clonedCase ->
          clonedCase.offenderCharges += sourceCase.offenderCharges.map { offenderCharge ->
            OffenderCharge(
              offenderBooking = latestBooking,
              offence = offenderCharge.offence,
              courtCase = clonedCase,
              offencesCount = offenderCharge.offencesCount,
              offenceDate = offenderCharge.offenceDate,
              offenceEndDate = offenderCharge.offenceEndDate,
              plea = offenderCharge.plea,
              propertyValue = offenderCharge.propertyValue,
              totalPropertyValue = offenderCharge.totalPropertyValue,
              cjitCode1 = offenderCharge.cjitCode1,
              cjitCode2 = offenderCharge.cjitCode2,
              cjitCode3 = offenderCharge.cjitCode3,
              chargeStatus = offenderCharge.chargeStatus,
              resultCode1 = offenderCharge.resultCode1,
              resultCode2 = offenderCharge.resultCode2,
              resultCode1Indicator = offenderCharge.resultCode1Indicator,
              resultCode2Indicator = offenderCharge.resultCode2Indicator,
              mostSeriousFlag = offenderCharge.mostSeriousFlag,
            )
          }
        },
      ).also { clonedCase ->
        clonedCase.courtEvents += sourceCase.courtEvents.map { courtEvent ->
          CourtEvent(
            offenderBooking = latestBooking,
            courtCase = clonedCase,
            eventDate = courtEvent.eventDate,
            startTime = courtEvent.startTime,
            courtEventType = courtEvent.courtEventType,
            judgeName = courtEvent.judgeName,
            eventStatus = courtEvent.eventStatus,
            court = courtEvent.court,
            outcomeReasonCode = courtEvent.outcomeReasonCode,
            commentText = courtEvent.commentText,
            nextEventRequestFlag = courtEvent.nextEventRequestFlag,
            orderRequestedFlag = courtEvent.orderRequestedFlag,
            nextEventDate = courtEvent.nextEventDate,
            nextEventStartTime = courtEvent.nextEventStartTime,
            directionCode = courtEvent.directionCode,
            holdFlag = courtEvent.holdFlag,
          ).also { clonedCourtEvent ->
            clonedCourtEvent.courtOrders += courtEvent.courtOrders.map { courtOrder ->
              CourtOrder(
                offenderBooking = latestBooking,
                courtCase = clonedCase,
                courtEvent = clonedCourtEvent,
                courtDate = courtOrder.courtDate,
                issuingCourt = courtOrder.issuingCourt,
                courtInfoId = courtOrder.courtInfoId,
                orderType = courtOrder.orderType,
                orderStatus = courtOrder.orderStatus,
                dueDate = courtOrder.dueDate,
                seriousnessLevel = courtOrder.seriousnessLevel,
                requestDate = courtOrder.requestDate,
                nonReportFlag = courtOrder.nonReportFlag,
                commentText = courtOrder.commentText,
              ).also { clonedCourtOrder ->
                clonedCourtOrder.sentencePurposes += courtOrder.sentencePurposes.map { sentencePurpose ->
                  SentencePurpose(
                    id = SentencePurposeId(
                      order = clonedCourtOrder,
                      orderPartyCode = sentencePurpose.id.orderPartyCode,
                      purposeCode = sentencePurpose.id.purposeCode,
                    ),
                  )
                }
              }
            }
          }
        }
        clonedCase.sentences += sourceCase.sentences.map { offenderSentence ->
          val lineSequence = offenderSentenceRepository.getNextLineSequence(latestBooking).toInt()
          val newSequence = offenderSentenceRepository.getNextSequence(latestBooking)
          val sourceCourtOrders = sourceCase.courtEvents.flatMap { it.courtOrders }
          val clonedCourtOrders = clonedCase.courtEvents.flatMap { it.courtOrders }
          offenderSentenceRepository.saveAndFlush(
            OffenderSentence(
              id = SentenceId(
                offenderBooking = latestBooking,
                sequence = newSequence,
              ),
              // make active
              status = "A",
              calculationType = offenderSentence.calculationType,
              courtOrder = clonedCourtOrders[sourceCourtOrders.indexOf(offenderSentence.courtOrder)],
              startDate = offenderSentence.startDate,
              // set to null then reset to real value once all cases have been cloned
              consecSequence = null,
              endDate = offenderSentence.endDate,
              commentText = offenderSentence.commentText,
              absenceCount = offenderSentence.absenceCount,
              courtCase = clonedCase,
              etdCalculatedDate = offenderSentence.etdCalculatedDate,
              mtdCalculatedDate = offenderSentence.mtdCalculatedDate,
              ltdCalculatedDate = offenderSentence.ltdCalculatedDate,
              ardCalculatedDate = offenderSentence.ardCalculatedDate,
              crdCalculatedDate = offenderSentence.crdCalculatedDate,
              pedCalculatedDate = offenderSentence.pedCalculatedDate,
              npdCalculatedDate = offenderSentence.npdCalculatedDate,
              ledCalculatedDate = offenderSentence.ledCalculatedDate,
              sedCalculatedDate = offenderSentence.sedCalculatedDate,
              prrdCalculatedDate = offenderSentence.prrdCalculatedDate,
              tariffCalculatedDate = offenderSentence.tariffCalculatedDate,
              dprrdCalculatedDate = offenderSentence.dprrdCalculatedDate,
              tusedCalculatedDate = offenderSentence.tusedCalculatedDate,
              aggSentenceSequence = offenderSentence.aggSentenceSequence,
              aggAdjustDays = offenderSentence.aggAdjustDays,
              sentenceLevel = offenderSentence.sentenceLevel,
              extendedDays = offenderSentence.extendedDays,
              counts = offenderSentence.counts,
              statusUpdateReason = offenderSentence.statusUpdateReason,
              statusUpdateComment = offenderSentence.statusUpdateComment,
              statusUpdateDate = offenderSentence.statusUpdateDate,
              statusUpdateStaff = offenderSentence.statusUpdateStaff,
              category = offenderSentence.category,
              fineAmount = offenderSentence.fineAmount,
              dischargeDate = offenderSentence.dischargeDate,
              nomSentDetailRef = offenderSentence.nomSentDetailRef,
              nomConsToSentDetailRef = offenderSentence.nomConsToSentDetailRef,
              nomConsFromSentDetailRef = offenderSentence.nomConsFromSentDetailRef,
              nomConsWithSentDetailRef = offenderSentence.nomConsWithSentDetailRef,
              lineSequence = lineSequence,
              hdcExclusionFlag = offenderSentence.hdcExclusionFlag,
              hdcExclusionReason = offenderSentence.hdcExclusionReason,
              cjaAct = offenderSentence.cjaAct,
              sled2Calc = offenderSentence.sled2Calc,
              startDate2Calc = offenderSentence.startDate2Calc,
              // TODO
              adjustments = mutableListOf(),
            ).also { clonedSentence ->
              clonedSentence.offenderSentenceCharges += offenderSentence.offenderSentenceCharges.map { sourceOffenderSentenceCharge ->
                val clonedOffenderSentenceCharge = clonedCase.offenderCharges[offenderSentence.offenderSentenceCharges.indexOf(sourceOffenderSentenceCharge)]
                OffenderSentenceCharge(
                  id = OffenderSentenceChargeId(
                    offenderBooking = latestBooking,
                    sequence = clonedSentence.id.sequence,
                    offenderChargeId = clonedOffenderSentenceCharge.id,
                  ),
                  offenderSentence = clonedSentence,
                  offenderCharge = clonedOffenderSentenceCharge,
                )
              }
            },
          ).also { clonedSentence ->
            clonedSentence.offenderSentenceTerms += offenderSentence.offenderSentenceTerms.map { sourceOffenderSentenceTerm ->
              val termSequence = offenderSentenceTermRepository.getNextTermSequence(
                offenderBookId = latestBooking.bookingId,
                sentenceSeq = clonedSentence.id.sequence,
              )
              offenderSentenceTermRepository.saveAndFlush(
                OffenderSentenceTerm(
                  id = OffenderSentenceTermId(
                    offenderBooking = latestBooking,
                    sentenceSequence = clonedSentence.id.sequence,
                    termSequence = termSequence,
                  ),
                  years = sourceOffenderSentenceTerm.years,
                  months = sourceOffenderSentenceTerm.months,
                  weeks = sourceOffenderSentenceTerm.weeks,
                  days = sourceOffenderSentenceTerm.days,
                  hours = sourceOffenderSentenceTerm.hours,
                  lifeSentenceFlag = sourceOffenderSentenceTerm.lifeSentenceFlag,
                  offenderSentence = clonedSentence,
                  startDate = sourceOffenderSentenceTerm.startDate,
                  endDate = sourceOffenderSentenceTerm.endDate,
                  sentenceTermType = sourceOffenderSentenceTerm.sentenceTermType,
                ),
              )
            }
          }
        }
      }
    }.let {
      val clonedCases = courtCaseRepository.saveAllAndFlush(it)
      // fix linked case
      clonedCases.forEachIndexed { caseIndex, clonedCase ->
        val sourceCase = sourceCourtCases[caseIndex]
        if (sourceCase.targetCombinedCase != null) {
          clonedCase.targetCombinedCase = clonedCases[sourceCourtCases.indexOf(sourceCase.targetCombinedCase)]
          clonedCase.targetCombinedCase!!.sourceCombinedCases += clonedCase

          with(clonedCase.targetCombinedCase!!) {
            // update target case so trigger for status update is ok
            this.statusUpdateDate = LocalDate.now()
            this.statusUpdateReason = if (this.caseStatus.code == "A") {
              // active
              "A"
            } else {
              // inactive
              "D"
            }
            this.statusUpdateStaff = findStaffByUsername(clonedCase.createUsername)
          }
        }
      }

      // copy court event charges since we can have some pointing at a linked case that is only copied to booking after all cases are copied
      val sourceOffenderCharges = sourceCourtCases.flatMap { it.offenderCharges }
      val clonedOffenderCharges = clonedCases.flatMap { it.offenderCharges }
      clonedCases.forEachIndexed { caseIndex, clonedCase ->
        clonedCase.courtEvents.forEachIndexed { courtEventIndex, clonedCourtEvent ->
          val sourceCourtEvent = sourceCourtCases[caseIndex].courtEvents[courtEventIndex]

          clonedCourtEvent.courtEventCharges += sourceCourtEvent.courtEventCharges.map { sourceCourtEventCharge ->
            courtEventChargeRepository.saveAndFlush(
              CourtEventCharge(
                id = CourtEventChargeId(
                  offenderCharge = clonedOffenderCharges[sourceOffenderCharges.indexOf(sourceCourtEventCharge.id.offenderCharge)],
                  courtEvent = clonedCourtEvent,
                ),
                offencesCount = sourceCourtEventCharge.offencesCount,
                offenceDate = sourceCourtEventCharge.offenceDate,
                offenceEndDate = sourceCourtEventCharge.offenceEndDate,
                plea = sourceCourtEventCharge.plea,
                propertyValue = sourceCourtEventCharge.propertyValue,
                totalPropertyValue = sourceCourtEventCharge.totalPropertyValue,
                cjitCode1 = sourceCourtEventCharge.cjitCode1,
                cjitCode2 = sourceCourtEventCharge.cjitCode2,
                cjitCode3 = sourceCourtEventCharge.cjitCode3,
                resultCode1 = sourceCourtEventCharge.resultCode1,
                resultCode2 = sourceCourtEventCharge.resultCode2,
                resultCode1Indicator = sourceCourtEventCharge.resultCode1Indicator,
                resultCode2Indicator = sourceCourtEventCharge.resultCode2Indicator,
                mostSeriousFlag = sourceCourtEventCharge.mostSeriousFlag,
              ),
            ).also { clonedCourtEventCharge ->
              if (sourceCourtEventCharge.linkedCaseTransaction != null) {
                clonedCourtEventCharge.linkedCaseTransaction = sourceCourtEventCharge.linkedCaseTransaction.let { sourceLinkedCaseTransaction ->
                  val sourceLinkedCase = clonedCases[sourceCourtCases.indexOf(sourceLinkedCaseTransaction!!.sourceCase)]
                  val linkCaseTxnId = LinkCaseTxnId(
                    caseId = sourceLinkedCase.id,
                    combinedCaseId = clonedCase.id,
                    offenderChargeId = clonedCourtEventCharge.id.offenderCharge.id,
                  )
                  linkCaseTxnRepository.saveAndFlush(
                    LinkCaseTxn(
                      id = linkCaseTxnId,
                      courtEventId = clonedCourtEvent.id,
                      courtEventCharge = clonedCourtEventCharge,
                      sourceCase = sourceLinkedCase,
                      targetCase = clonedCase,
                      offenderCharge = clonedCourtEventCharge.id.offenderCharge,
                      courtEvent = clonedCourtEvent,
                    ),
                  )
                }
              }
            }
          }
        }
      }

      // fix up consecutive sequence numbers
      val sourceSentences = sourceCourtCases.flatMap { it.sentences }
      val clonedSentences = clonedCases.flatMap { it.sentences }

      sourceSentences.forEachIndexed { index, sourceSentence ->
        if (sourceSentence.consecutiveSentence != null) {
          val clonedSentence = clonedSentences[index]
          val nextSentenceIndex = sourceSentences.indexOf(sourceSentence.consecutiveSentence)
          clonedSentence.consecutiveSentence = clonedSentences[nextSentenceIndex]
          clonedSentence.consecSequence = clonedSentence.consecutiveSentence!!.id.sequence.toInt()
        }
      }

      // fix up case identifiers; this needs doing at the end to avoid NOMIS trigger seeing an insert and update to offender cases and trying
      // to insert the primary identifier into the case identifier table twice
      courtCaseRepository.saveAllAndFlush(clonedCases).also { clonedCases ->
        clonedCases.forEachIndexed { caseIndex, clonedCase ->
          val sourceCase = sourceCourtCases[caseIndex]
          clonedCase.primaryCaseInfoNumber = sourceCase.primaryCaseInfoNumber
          // NOMIS trigger will insert primaryCaseInfoNumber - so exclude from our manually insert else we will have duplicate keys
          clonedCase.caseInfoNumbers += sourceCase.caseInfoNumbers.filterNot { caseIdentifier -> caseIdentifier.id.identifierType == "CASE/INFO#" && caseIdentifier.id.reference == sourceCase.primaryCaseInfoNumber }.map { caseInfoNumber ->
            OffenderCaseIdentifier(
              id = OffenderCaseIdentifierPK(
                identifierType = caseInfoNumber.id.identifierType,
                reference = caseInfoNumber.id.reference,
                courtCase = clonedCase,
              ),
            )
          }
        }
      }

      courtCaseRepository.saveAllAndFlush(clonedCases).also {
        storedProcedureRepository.imprisonmentStatusUpdate(
          bookingId = latestBooking.bookingId,
          changeType = ImprisonmentStatusChangeType.UPDATE_RESULT.name,
        )
      }
    }.zip(sourceCourtCases)

    return BookingCourtCaseCloneResponse(
      courtCases = clonedCasesWithSource.map { (cloned, source) ->
        ClonedCourtCaseResponse(
          courtCase = cloned.toCourtCaseResponse(),
          sourceCourtCase = source.toCourtCaseResponse(),
        )
      },
    )
  }
}

private fun OffenderChargeRequest.toExistingOffenderChargeRequest(chargeId: Long): ExistingOffenderChargeRequest = ExistingOffenderChargeRequest(
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
  combinedCaseId = this.targetCombinedCase?.id,
  sourceCombinedCaseIds = this.sourceCombinedCases.map { it.id },
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
  sentences = this.sentences.map { it.toSentenceResponse() },
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
  resultCode1 = this.resultCode1?.toOffenceResultCodeResponse(),
  resultCode2 = this.resultCode2?.toOffenceResultCodeResponse(),
  mostSeriousFlag = this.mostSeriousFlag,
  lidsOffenceNumber = this.lidsOffenceNumber,
)

private fun OffenceResultCode.toOffenceResultCodeResponse(): OffenceResultCodeResponse = OffenceResultCodeResponse(
  code = this.code,
  description = this.description,
  chargeStatus = this.chargeStatus,
  dispositionCode = this.dispositionCode,
  conviction = this.conviction,
)

private fun Offence.toOffence(): OffenceResponse = OffenceResponse(
  offenceCode = this.id.offenceCode,
  statuteCode = this.id.statuteCode,
  description = this.description,
)

private fun CourtEventCharge.toCourtEventCharge(): CourtEventChargeResponse = CourtEventChargeResponse(
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
  resultCode1 = this.resultCode1?.toOffenceResultCodeResponse(),
  resultCode2 = this.resultCode2?.toOffenceResultCodeResponse(),
  mostSeriousFlag = this.mostSeriousFlag,
  linkedCaseDetails = this.linkedCaseTransaction?.toLinkedCaseDetails(),
)

private fun LinkCaseTxn.toLinkedCaseDetails(): LinkedCaseChargeDetails = LinkedCaseChargeDetails(
  caseId = this.sourceCase.id,
  eventId = this.courtEvent.id,
  dateLinked = this.createDatetime.toLocalDate(),
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
  outcomeReasonCode = this.outcomeReasonCode?.toOffenceResultCodeResponse(),
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

private fun CourtOrder.toCourtOrder(): CourtOrderResponse = CourtOrderResponse(
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
  eventId = this.courtEvent!!.id,
)

private fun SentencePurpose.toSentencePurpose(): SentencePurposeResponse = SentencePurposeResponse(
  orderId = this.id.order.id,
  orderPartyCode = this.id.orderPartyCode,
  purposeCode = this.id.purposeCode,
)

private fun OffenderSentenceTerm.toSentenceTermResponse(): SentenceTermResponse = SentenceTermResponse(
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
  prisonId = this.id.offenderBooking.location.id,
)

fun OffenderSentence.toSentenceResponse(): SentenceResponse = SentenceResponse(
  bookingId = this.id.offenderBooking.bookingId,
  sentenceSeq = this.id.sequence,
  status = this.status,
  calculationType = CodeDescription(
    code = this.calculationType.id.calculationType,
    description = this.calculationType.description,
  ),
  startDate = this.startDate,
  // no referential integrity on courtEvent
  courtOrder = this.courtOrder?.takeIf { it.courtEvent != null }?.toCourtOrder(),
  consecSequence = this.consecutiveSentence?.id?.sequence?.toInt(),
  endDate = this.endDate,
  commentText = this.commentText,
  absenceCount = this.absenceCount,
  caseId = this.courtCase?.id,
  etdCalculatedDate = this.etdCalculatedDate,
  mtdCalculatedDate = this.mtdCalculatedDate,
  ltdCalculatedDate = this.ltdCalculatedDate,
  ardCalculatedDate = this.ardCalculatedDate,
  crdCalculatedDate = this.crdCalculatedDate,
  pedCalculatedDate = this.pedCalculatedDate,
  npdCalculatedDate = this.npdCalculatedDate,
  ledCalculatedDate = this.ledCalculatedDate,
  sedCalculatedDate = this.sedCalculatedDate,
  prrdCalculatedDate = this.prrdCalculatedDate,
  tariffCalculatedDate = this.tariffCalculatedDate,
  dprrdCalculatedDate = this.dprrdCalculatedDate,
  tusedCalculatedDate = this.tusedCalculatedDate,
  aggSentenceSequence = this.aggSentenceSequence,
  aggAdjustDays = this.aggAdjustDays,
  sentenceLevel = this.sentenceLevel,
  extendedDays = this.extendedDays,
  counts = this.counts,
  statusUpdateReason = this.statusUpdateReason,
  statusUpdateComment = this.statusUpdateComment,
  statusUpdateDate = this.statusUpdateDate,
  statusUpdateStaffId = this.statusUpdateStaff?.id,
  category = this.category.toCodeDescription(),
  fineAmount = this.fineAmount,
  dischargeDate = this.dischargeDate,
  nomSentDetailRef = this.nomSentDetailRef,
  nomConsToSentDetailRef = this.nomConsToSentDetailRef,
  nomConsFromSentDetailRef = this.nomConsFromSentDetailRef,
  nomConsWithSentDetailRef = this.nomConsWithSentDetailRef,
  lineSequence = this.lineSequence,
  hdcExclusionFlag = this.hdcExclusionFlag,
  hdcExclusionReason = this.hdcExclusionReason,
  cjaAct = this.cjaAct,
  sled2Calc = this.sled2Calc,
  startDate2Calc = this.startDate2Calc,
  createdDateTime = this.createDatetime,
  createdByUsername = this.createUsername,
  sentenceTerms = this.offenderSentenceTerms.map { it.toSentenceTermResponse() },
  offenderCharges = this.offenderSentenceCharges.map { it.offenderCharge.toOffenderCharge() },
  missingCourtOffenderChargeIds = emptyList(),
  prisonId = this.id.offenderBooking.location.id,
  recallCustodyDate = this.id.offenderBooking.fixedTermRecall?.takeIf { this.isActiveRecallSentence() }?.let {
    RecallCustodyDate(
      returnToCustodyDate = it.returnToCustodyDate,
      recallLength = it.recallLength,
      comments = it.comments,
    )
  },
)

fun OffenderSentence.isActiveRecallSentence() = this.status == "A" && this.isRecallSentence()
fun OffenderSentence.isRecallSentence() = this.calculationType.isRecallSentence()

fun SentenceCalculationType.isRecallSentence() = with(this.id.calculationType) {
  startsWith("LR") ||
    contains("FTR") ||
    this in listOf("CUR", "CUR_ORA", "HDR", "HDR_ORA")
}
