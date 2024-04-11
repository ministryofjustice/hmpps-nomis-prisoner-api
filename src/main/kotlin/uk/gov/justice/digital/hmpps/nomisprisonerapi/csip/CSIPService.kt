package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPInterview
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPPlan
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReportRepository
import java.time.LocalDateTime

@Service
@Transactional
class CSIPService(
  private val csipRepository: CSIPReportRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getCSIP(csipId: Long): CSIPResponse? {
    return csipRepository.findByIdOrNull(csipId)?.toCSIPResponse()
      ?: throw NotFoundException("CSIP with id=$csipId does not exist")
  }

  fun findIdsByFilter(pageRequest: Pageable, csipFilter: CSIPFilter): Page<CSIPIdResponse> {
    log.info("CSIP Id filter request : $csipFilter with page request $pageRequest")
    return findAllIds(
      fromDate = csipFilter.fromDate?.atStartOfDay(),
      toDate = csipFilter.toDate?.plusDays(1)?.atStartOfDay(),
      pageRequest,
    ).map { CSIPIdResponse(it) }
  }

  fun findAllIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageRequest: Pageable,
  ): Page<Long> =

    if (fromDate == null && toDate == null) {
      csipRepository.findAllCSIPIds(pageRequest)
    } else {
      // optimisation: only do the complex SQL if we have a filter
      // typically we won't when run in production
      csipRepository.findAllCSIPIds(fromDate, toDate, pageRequest)
    }

  fun getCSIPCount(): Long = csipRepository.count()
}

private fun CSIPReport.toCSIPResponse(): CSIPResponse =
  CSIPResponse(
    id = id,
    offender = offenderBooking.offender.toOffender(),
    bookingId = offenderBooking.bookingId,
    incidentDateTime = incidentTime?.toLocalTime()?.atDate(incidentDate),
    type = type.toCodeDescription(),
    location = location.toCodeDescription(),
    areaOfWork = areaOfWork.toCodeDescription(),
    reportedBy = reportedBy,
    reportedDate = reportedDate,
    logNumber = logNumber,
    proActiveReferral = proActiveReferral,
    staffAssaulted = staffAssaulted,
    staffAssaultedName = staffAssaultedName,
    reportDetails = toReportDetailResponse(),
    saferCustodyScreening = toSCSResponse(),
    investigation = toInvestigationResponse(),
    plans = plans.map { it.toPlanResponse() },
  )

private fun Offender.toOffender() =
  Offender(
    offenderNo = nomsId,
    firstName = firstName,
    lastName = lastName,
  )

private fun CSIPReport.toReportDetailResponse() =
  ReportDetails(
    involvement = involvement?.toCodeDescription(),
    concern = concernDescription,
    knownReasons = knownReasons,
    otherInformation = otherInformation,
    factors = factors.map { FactorResponse(id = it.id, type = it.type.toCodeDescription(), comment = it.comment) },
  )

private fun CSIPReport.toSCSResponse() =
  SaferCustodyScreening(
    outcome = outcome?.toCodeDescription(),
    recordedBy = outcomeCreateUsername,
    recordedDate = outcomeCreateDate,
    reasonForDecision = reasonForDecision,
  )

private fun CSIPReport.toInvestigationResponse() =
  Investigation(
    staffInvolved = staffInvolved,
    evidenceSecured = evidenceSecured,
    reasonOccurred = reasonOccurred,
    usualBehaviour = usualBehaviour,
    trigger = trigger,
    protectiveFactors = protectiveFactors,
    interviews = interviews.map { it.toInterviewResponse() },
  )
private fun CSIPInterview.toInterviewResponse() =
  InterviewDetails(
    interviewee = interviewee,
    date = interviewDate,
    role = role.toCodeDescription(),
    comments = comments,
  )

private fun CSIPPlan.toPlanResponse() =
  Plan(
    id = id,
    identifiedNeed = identifiedNeed,
    intervention = intervention,
    referredBy = referredBy,
    progression = progression,
    createdDate = createDate,
    targetDate = targetDate,
    closedDate = closedDate,
  )
