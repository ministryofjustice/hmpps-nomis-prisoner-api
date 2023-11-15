package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
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
)
