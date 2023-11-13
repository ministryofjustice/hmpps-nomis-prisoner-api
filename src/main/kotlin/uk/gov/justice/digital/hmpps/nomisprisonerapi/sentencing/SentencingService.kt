package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
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

    return courtCaseRepository.findByIdOrNull(id)?.let {
      CourtCaseResponse(
        id = it.id,
        offenderNo = it.offenderBooking.offender.nomsId,
        caseInfoNumber = it.caseInfoNumber,
        caseSequence = it.caseSequence,
        caseStatus = it.caseStatus.toCodeDescription(),
        caseType = it.legalCaseType.toCodeDescription(),
        beginDate = it.beginDate,
        prisonId = it.prison.id,
        combinedCaseId = it.combinedCase?.id,
        lidsCaseNumber = it.lidsCaseNumber,
        lidsCaseId = it.lidsCaseId,
        lidsCombinedCaseId = it.lidsCombinedCaseId,
        statusUpdateReason = it.statusUpdateReason,
        statusUpdateComment = it.statusUpdateComment,
        statusUpdateDate = it.statusUpdateDate,
        statusUpdateStaffId = it.statusUpdateStaff?.id,
        createdDateTime = it.createDatetime,
        createdByUsername = it.createUsername
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
