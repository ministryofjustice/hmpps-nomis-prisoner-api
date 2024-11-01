package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.DocumentIdResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPAttendee
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPFactor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPInterview
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPPlan
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReview
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount

fun CSIPReport.toCSIPResponse(documentIds: List<DocumentIdResponse>? = null): CSIPResponse =
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
    createdBy = createUsername,
    createDateTime = createDatetime,
    lastModifiedBy = lastModifiedUsername,
    lastModifiedDateTime = lastModifiedDateTime,
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
    referralCompletedByDisplayName = referralCompletedByStaffUserAccount.asDisplayName(),
    referralCompletedDate = referralCompletedDate,
  )

private fun CSIPReport.toSCSResponse() =
  SaferCustodyScreening(
    outcome = outcome?.toCodeDescription(),
    recordedBy = outcomeCreateUsername,
    recordedByDisplayName = outcomeCreatedByStaffUserAccount.asDisplayName(),
    recordedDate = outcomeCreateDate,
    reasonForDecision = reasonForDecision,
  )

fun CSIPFactor.toFactorResponse() =
  CSIPFactorResponse(
    id = id,
    type = type.toCodeDescription(),
    comment = comment,
    createDateTime = createDatetime,
    createdBy = createUsername,
    lastModifiedDateTime = lastModifiedDateTime,
    lastModifiedBy = lastModifiedUsername,
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
    id = id,
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
    recordedByDisplayName = recordedByStaffUserAccount.asDisplayName(),
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
    recordedDate = recordedDate!!,
    recordedBy = recordedUser!!,
    recordedByDisplayName = recordedByStaffUserAccount.asDisplayName(),
    createDateTime = createDateTime,
    createdBy = createUsername,
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

private fun StaffUserAccount?.asDisplayName(): String? = this?.staff?.let { "${it.firstName} ${it.lastName}" }
