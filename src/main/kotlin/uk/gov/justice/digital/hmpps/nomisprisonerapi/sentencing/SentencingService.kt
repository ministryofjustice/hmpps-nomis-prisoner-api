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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceTerm
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentencePurpose
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceResultCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderChargeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.findRootByNomisId
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional
class SentencingService(
  private val courtCaseRepository: CourtCaseRepository,
  private val offenderSentenceRepository: OffenderSentenceRepository,
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val telemetryClient: TelemetryClient,
  private val legalCaseTypeRepository: ReferenceCodeRepository<LegalCaseType>,
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
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getCourtCase(id: Long, offenderNo: String): CourtCaseResponse {
    findPrisoner(offenderNo).findLatestBooking()

    return courtCaseRepository.findByIdOrNull(id)?.toCourtCaseResponse()
      ?: throw NotFoundException("Court case $id not found")
  }

  fun getCourtCasesByOffender(offenderNo: String): List<CourtCaseResponse> {
    findPrisoner(offenderNo).findLatestBooking()

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
    findPrisoner(offenderNo).findLatestBooking().let { booking ->

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
              prison = lookupEstablishment(courtAppearanceRequest.courtId),
              outcomeReasonCode = courtAppearanceRequest.outcomeReasonCode?.let { lookupOffenceResultCode(it) },
              nextEventDate = courtAppearanceRequest.nextEventDateTime?.toLocalDate(),
              nextEventStartTime = courtAppearanceRequest.nextEventDateTime,
              directionCode = lookupDirectionType(DirectionType.OUT),
            ).also { courtEvent ->
              courtAppearanceRequest.courtEventCharges.map { offenderChargeRequest ->
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
      courtCaseRepository.saveAndFlush(courtCase)
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

  @Audit
  fun createCourtAppearance(
    offenderNo: String,
    caseId: Long,
    request: CreateCourtAppearanceRequest,
  ): CreateCourtAppearanceResponse = findPrisoner(offenderNo).findLatestBooking().let { booking ->
    val courtAppearanceRequest = request.courtAppearance
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
        prison = lookupEstablishment(courtAppearanceRequest.courtId),
        outcomeReasonCode = courtAppearanceRequest.outcomeReasonCode?.let { lookupOffenceResultCode(it) },
        nextEventDate = courtAppearanceRequest.nextEventDateTime?.toLocalDate(),
        nextEventStartTime = courtAppearanceRequest.nextEventDateTime,
        directionCode = lookupDirectionType(DirectionType.OUT),
      ).also { courtEvent ->
        request.existingOffenderChargeIds.map { offenderChargeId ->
          getOffenderCharge(offenderChargeId).let { offenderCharge ->
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
      courtCase.courtEvents.add(
        courtEvent,
      )
      courtEventRepository.saveAndFlush(courtEvent)
    }.let { courtEvent ->
      var nextAppearanceId: Long? = null
      courtEvent.nextEventDate?.let {
        courtEvent.courtCase!!.courtEvents.add(
          createNextCourtEvent(booking, courtEvent, request.courtAppearance).also { nextCourtEvent ->
            nextCourtEvent.initialiseCourtEventCharges()
            nextAppearanceId = courtEventRepository.saveAndFlush(nextCourtEvent).id
          },
        )
      }
      CreateCourtAppearanceResponse(
        id = courtEvent.id,
        courtEventChargesIds = courtEvent.courtEventCharges.map { courtEventCharge ->
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
          ),
          null,
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
    prison = lookupEstablishment(request.nextCourtId!!),
    directionCode = lookupDirectionType(DirectionType.OUT),
  )

  @Audit
  fun updateCourtAppearance(
    offenderNo: String,
    caseId: Long,
    eventId: Long,
    request: UpdateCourtAppearanceRequest,
  ) {
    findPrisoner(offenderNo).let {
      findCourtCase(caseId, offenderNo).let { courtCase ->
        findCourtAppearance(eventId, offenderNo).let { courtAppearance ->
          courtAppearance.eventDate = request.eventDateTime.toLocalDate()
          courtAppearance.startTime = request.eventDateTime
          courtAppearance.courtEventType = lookupMovementReasonType(request.courtEventType)
          courtAppearance.eventStatus = determineEventStatus(
            request.eventDateTime.toLocalDate(),
            courtCase.offenderBooking,
          )
          courtAppearance.prison = lookupEstablishment(request.courtId)
          courtAppearance.outcomeReasonCode =
            request.outcomeReasonCode?.let { lookupOffenceResultCode(it) }
          // will get a separate update for a generated next event - so just updating these fields rather than the target appearance
          courtAppearance.nextEventDate = request.nextEventDateTime?.toLocalDate()
          courtAppearance.nextEventStartTime = request.nextEventDateTime

          val chargesToUpdateMap = request.courtEventChargesToUpdate.map { it.offenderChargeId to it }.toMap()
          courtAppearance.courtEventCharges.filter { chargesToUpdateMap.contains(it.id.offenderCharge.id) }
            .map { courtEventCharge ->
              val updateCharge = chargesToUpdateMap.getValue(courtEventCharge.id.offenderCharge.id)
              val resultCode = updateCharge.resultCode1?.let { rs -> lookupOffenceResultCode(rs) }
              courtEventCharge.offenceDate = updateCharge.offenceDate
              courtEventCharge.offenceEndDate = updateCharge.offenceEndDate
              courtEventCharge.mostSeriousFlag = updateCharge.mostSeriousFlag
              courtEventCharge.offencesCount = updateCharge.offencesCount
              courtEventCharge.resultCode1 = resultCode
              courtEventCharge.resultCode1Indicator = resultCode?.dispositionCode
              with(courtEventCharge.id.offenderCharge) {
                offenceDate = updateCharge.offenceDate
                offenceEndDate = updateCharge.offenceEndDate
                offence = lookupOffence(updateCharge.offenceCode)
                // TODO setting of result code related data is more complex than this - main SP is TAG_LEGAL_CASES.pkg
                resultCode1 = resultCode?.let { it }
                resultCode1Indicator = resultCode?.dispositionCode
                chargeStatus = resultCode?.chargeStatus?.let { lookupChargeStatusType(it) }
                mostSeriousFlag = updateCharge.mostSeriousFlag
                offencesCount = updateCharge.offencesCount
              }
              courtEventCharge
            }.let {
              courtAppearance.courtEventCharges.clear()
              courtAppearance.courtEventCharges.addAll(it)
            }
        }

        courtCase.getOffenderChargesNotAssociatedWithCourtAppearances().forEach {
          courtCase.offenderCharges.remove(it)
          log.debug("Offender charge deleted: ${it.id}")
        }

        telemetryClient.trackEvent(
          "court-appearance-updated",
          mapOf(
            "courtCaseId" to caseId.toString(),
            "bookingId" to courtCase.offenderBooking.bookingId.toString(),
            "offenderNo" to offenderNo,
            "court" to request.courtId,
            "courtEventId" to eventId.toString(),
          ),
          null,
        )
      }
    }
  }

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

  private fun Offender.findLatestBooking(): OffenderBooking {
    return this.bookings.firstOrNull { it.bookingSequence == 1 }
      ?: throw BadDataException("Prisoner ${this.nomsId} has no bookings")
  }

  private fun findPrisoner(offenderNo: String): Offender {
    return offenderRepository.findRootByNomisId(offenderNo)
      ?: throw NotFoundException("Prisoner $offenderNo not found")
  }

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

  private fun lookupLegalCaseType(code: String): LegalCaseType = legalCaseTypeRepository.findByIdOrNull(
    LegalCaseType.pk(code),
  ) ?: throw BadDataException("Legal Case Type $code not found")

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
  offenderNo = this.offenderBooking.offender.nomsId,
  eventDateTime = LocalDateTime.of(this.eventDate, this.startTime.toLocalTime()),
  courtEventType = this.courtEventType.toCodeDescription(),
  eventStatus = this.eventStatus.toCodeDescription(),
  directionCode = this.directionCode?.toCodeDescription(),
  judgeName = this.judgeName,
  courtId = this.prison.id,
  outcomeReasonCode = this.outcomeReasonCode?.toCodeDescription(),
  commentText = this.commentText,
  orderRequestedFlag = this.orderRequestedFlag,
  holdFlag = this.holdFlag,
  nextEventRequestFlag = this.nextEventRequestFlag,
  nextEventDateTime = this.nextEventDate?.let {
    this.nextEventStartTime.let {
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
