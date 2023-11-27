package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.findRootByNomisId

@Service
@Transactional
class SentencingService(
  private val courtCaseRepository: CourtCaseRepository,
  private val offenderRepository: OffenderRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun getCourtCase(id: Long, offenderNo: String): CourtCaseResponse {
    findPrisoner(offenderNo).findLatestBooking()

    return courtCaseRepository.findByIdOrNull(id)?.let { courtCase ->
      CourtCaseResponse(
        id = courtCase.id,
        offenderNo = courtCase.offenderBooking.offender.nomsId,
        caseInfoNumber = courtCase.caseInfoNumber,
        caseSequence = courtCase.caseSequence,
        caseStatus = courtCase.caseStatus.toCodeDescription(),
        caseType = courtCase.legalCaseType.toCodeDescription(),
        beginDate = courtCase.beginDate,
        prisonId = courtCase.prison.id,
        combinedCaseId = courtCase.combinedCase?.id,
        lidsCaseNumber = courtCase.lidsCaseNumber,
        lidsCaseId = courtCase.lidsCaseId,
        lidsCombinedCaseId = courtCase.lidsCombinedCaseId,
        statusUpdateReason = courtCase.statusUpdateReason,
        statusUpdateComment = courtCase.statusUpdateComment,
        statusUpdateDate = courtCase.statusUpdateDate,
        statusUpdateStaffId = courtCase.statusUpdateStaff?.id,
        createdDateTime = courtCase.createDatetime,
        createdByUsername = courtCase.createUsername,
        courtEvents = courtCase.courtEvents.map { it.toCourtEvent() },
        offenderCharges = courtCase.offenderCharges.map { it.toOffenderCharge() },
      )
    } ?: throw NotFoundException("Court case $id not found")
  }

  private fun Offender.findLatestBooking(): OffenderBooking {
    return this.bookings.firstOrNull { it.bookingSequence == 1 }
      ?: throw BadDataException("Prisoner ${this.nomsId} has no bookings")
  }

  private fun findPrisoner(offenderNo: String): Offender {
    return offenderRepository.findRootByNomisId(offenderNo)
      ?: throw NotFoundException("Prisoner $offenderNo not found")
  }
}

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
)
