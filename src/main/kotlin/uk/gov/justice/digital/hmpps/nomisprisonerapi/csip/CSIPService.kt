package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.DocumentIdResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.DocumentService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.factors.toFactorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPAttendee
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPInterview
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPPlan
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReview
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReportRepository
import java.time.LocalDateTime

@Service
@Transactional
class CSIPService(
  private val csipRepository: CSIPReportRepository,
  private val documentService: DocumentService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val csipTemplates = listOf("CSIPA1_HMP", "CSIPA1_FNP", "CSIPA2_HMP", "CSIPA2_FNP", "CSIPA3_HMP", "CSIPA3_FNP")
  }

  fun getCSIP(csipId: Long, includeDocumentIds: Boolean): CSIPResponse? {
    val csip = csipRepository.findByIdOrNull(csipId)
      ?: throw NotFoundException("CSIP with id=$csipId does not exist")

    val documentIds = if (includeDocumentIds) {
      documentService.findAllIds(csip.offenderBooking.bookingId, csipTemplates)
    } else {
      null
    }
    return csip.toCSIPResponse(documentIds)
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

  fun deleteCSIP(csipId: Long) {
    csipRepository.findByIdOrNull(csipId)?.also {
      csipRepository.deleteById(csipId)
    }
      ?: log.info("CSIP deletion request for: $csipId ignored. CSIP does not exist")
  }
}

private fun CSIPReport.toCSIPResponse(documentIds: List<DocumentIdResponse>?): CSIPResponse =
  CSIPResponse(
    id = id,
    offender = offenderBooking.offender.toOffender(),
    bookingId = offenderBooking.bookingId,
    originalAgencyId = originalAgencyId,
    incidentDate = incidentDate,
    incidentTime = incidentTime?.toLocalTime(),
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

    caseManager = caseManager,
    planReason = reasonForPlan,
    firstCaseReviewDate = firstCaseReviewDate,
    decision = toDecisionResponse(),
    plans = plans.map { it.toPlanResponse() },
    reviews = reviews.map { it.toReviewResponse() },
    documents = documentIds,
    createDateTime = createDatetime,
    createdBy = createUsername,
    lastModifiedDateTime = lastModifiedDateTime,
    lastModifiedBy = lastModifiedUsername,
  )

private fun Offender.toOffender() =
  Offender(
    offenderNo = nomsId,
    firstName = firstName,
    lastName = lastName,
  )

private fun CSIPReport.toReportDetailResponse() =
  ReportDetails(
    releaseDate = releaseDate,
    involvement = involvement?.toCodeDescription(),
    concern = concernDescription,
    factors = factors.map { it.toFactorResponse() },
    knownReasons = knownReasons,
    otherInformation = otherInformation,
    saferCustodyTeamInformed = saferCustodyTeamInformed,
    referralComplete = referralComplete,
    referralCompletedBy = referralCompletedBy,
    referralCompletedDate = referralCompletedDate,
  )

private fun CSIPReport.toSCSResponse() =
  SaferCustodyScreening(
    outcome = outcome?.toCodeDescription(),
    recordedBy = outcomeCreateUsername,
    recordedDate = outcomeCreateDate,
    reasonForDecision = reasonForDecision,
  )

private fun CSIPReport.toInvestigationResponse() =
  InvestigationDetails(
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
    createDateTime = createDatetime,
    createdBy = createUsername,
    lastModifiedDateTime = lastModifiedDateTime,
    lastModifiedBy = lastModifiedUsername,
  )

private fun CSIPReport.toDecisionResponse() =
  Decision(
    conclusion = conclusion,
    decisionOutcome = decisionOutcome?.toCodeDescription(),
    signedOffRole = signedOffRole?.toCodeDescription(),
    recordedBy = recordedBy,
    recordedByDisplayName = recordedByStaffUserAccount?.staff.asDisplayName(),
    recordedDate = recordedDate,
    nextSteps = nextSteps,
    otherDetails = otherDetails,
    actions = toActionsResponse(),
  )

private fun CSIPReport.toActionsResponse() =
  Actions(
    openCSIPAlert = openCSIPAlert,
    nonAssociationsUpdated = nonAssociationsUpdated,
    observationBook = observationBook,
    unitOrCellMove = unitOrCellMove,
    csraOrRsraReview = csraOrRsraReview,
    serviceReferral = serviceReferral,
    simReferral = simReferral,
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
    createDateTime = createDatetime,
    createdBy = createUsername,
    lastModifiedDateTime = lastModifiedDateTime,
    lastModifiedBy = lastModifiedUsername,
  )

private fun CSIPReview.toReviewResponse() =
  Review(
    id = id,
    reviewSequence = reviewSequence,
    attendees = attendees.map { it.toAttendeeResponse() },
    remainOnCSIP = remainOnCSIP,
    csipUpdated = csipUpdated,
    caseNote = caseNote,
    closeCSIP = closeCSIP,
    peopleInformed = peopleInformed,
    summary = summary,
    nextReviewDate = nextReviewDate,
    closeDate = closeDate,
    createDateTime = createDatetime,
    createdBy = createUsername,
    createdByDisplayName = createdByStaffUserAccount?.staff.asDisplayName(),
    lastModifiedDateTime = lastModifiedDateTime,
    lastModifiedBy = lastModifiedUsername,
  )

private fun CSIPAttendee.toAttendeeResponse() =
  Attendee(
    id = id,
    name = name,
    role = role,
    attended = attended,
    contribution = contribution,
    createDateTime = createDatetime,
    createdBy = createUsername,
    lastModifiedDateTime = lastModifiedDateTime,
    lastModifiedBy = lastModifiedUsername,
  )

private fun Staff?.asDisplayName(): String? = this?.let { "${it.firstName} ${it.lastName}" }
