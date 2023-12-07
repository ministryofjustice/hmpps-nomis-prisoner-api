package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentencePurpose
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.findRootByNomisId

@Service
@Transactional
class SentencingService(
  private val courtCaseRepository: CourtCaseRepository,
  private val offenderSentenceRepository: OffenderSentenceRepository,
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun getCourtCase(id: Long, offenderNo: String): CourtCaseResponse {
    findPrisoner(offenderNo).findLatestBooking()

    return courtCaseRepository.findByIdOrNull(id)?.toCourtCaseResponse()
      ?: throw NotFoundException("Court case $id not found")
  }

  fun getCourtCasesByOffender(offenderNo: String): List<CourtCaseResponse> {
    findPrisoner(offenderNo).findLatestBooking()

    return courtCaseRepository.findByOffenderBooking_offender_nomsIdOrderByCreateDatetimeDesc(offenderNo)
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
      )
    }
      ?: throw NotFoundException("Offender sentence for booking ${offenderBooking.bookingId} and sentence sequence $sentenceSequence not found")
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
}

private fun CourtCase.toCourtCaseResponse(): CourtCaseResponse = CourtCaseResponse(
  id = this.id,
  offenderNo = this.offenderBooking.offender.nomsId,
  bookingId = this.offenderBooking.bookingId,
  caseInfoNumber = this.caseInfoNumber,
  caseSequence = this.caseSequence,
  caseStatus = this.caseStatus.toCodeDescription(),
  caseType = this.legalCaseType.toCodeDescription(),
  beginDate = this.beginDate,
  prisonId = this.prison.id,
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
    offenderChargeId = this.id.offenderCharge.id,
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
  eventDate = this.eventDate,
  startTime = this.startTime,
  courtEventType = this.courtEventType.toCodeDescription(),
  eventStatus = this.eventStatus.toCodeDescription(),
  directionCode = this.directionCode?.toCodeDescription(),
  judgeName = this.judgeName,
  prisonId = this.prison.id,
  outcomeReasonCode = this.outcomeReasonCode,
  commentText = this.commentText,
  orderRequestedFlag = this.orderRequestedFlag,
  holdFlag = this.holdFlag,
  nextEventRequestFlag = this.nextEventRequestFlag,
  nextEventDate = this.nextEventDate,
  nextEventStartTime = this.nextEventStartTime,
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
