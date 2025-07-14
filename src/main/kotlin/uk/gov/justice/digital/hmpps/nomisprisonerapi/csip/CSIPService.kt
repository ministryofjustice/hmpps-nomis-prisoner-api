package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.DocumentService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPComponent.Component.ATTENDEE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPComponent.Component.FACTOR
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPComponent.Component.INTERVIEW
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPComponent.Component.PLAN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPComponent.Component.REVIEW
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.trackEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.truncateToUtf8Length
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPAreaOfWork
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPAttendee
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPChild
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPFactor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPFactorType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPIncidentLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPIncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPInterview
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPInterviewRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPInvolvement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPPlan
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReview
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPSignedOffRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReportRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReviewRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import java.time.LocalDateTime

@Service
@Transactional
class CSIPService(
  private val csipRepository: CSIPReportRepository,
  private val documentService: DocumentService,
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val telemetryClient: TelemetryClient,
  val areaOfWorkRepository: ReferenceCodeRepository<CSIPAreaOfWork>,
  val factorRepository: ReferenceCodeRepository<CSIPFactorType>,
  val interviewRoleRepository: ReferenceCodeRepository<CSIPInterviewRole>,
  val involvementRepository: ReferenceCodeRepository<CSIPInvolvement>,
  val locationRepository: ReferenceCodeRepository<CSIPIncidentLocation>,
  val signedOffRoleRepository: ReferenceCodeRepository<CSIPSignedOffRole>,
  val outcomeRepository: ReferenceCodeRepository<CSIPOutcome>,
  val typeRepository: ReferenceCodeRepository<CSIPIncidentType>,
  val reviewRepository: CSIPReviewRepository,
  val staffUserAccountRepository: StaffUserAccountRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val csipTemplates = listOf("CSIPA1_HMP", "CSIPA1_FNP", "CSIPA2_HMP", "CSIPA2_FNP", "CSIPA3_HMP", "CSIPA3_FNP")
  }

  fun upsertCSIP(request: UpsertCSIPRequest): UpsertCSIPResponse {
    val toCreate = request.id == null
    val csipReport = if (toCreate) {
      request.toCreateCSIPReport()
    } else {
      updateCSIPReport(request)
    }

    return csipRepository.save(csipReport)
      .let {
        UpsertCSIPResponse(
          nomisCSIPReportId = it.id,
          offenderNo = it.offenderBooking.offender.nomsId,
          components = it.identifyNewComponents(request),
        )
      }.also {
        telemetryClient.trackEvent(
          "csip-${if (toCreate) "created" else "updated"}",
          mapOf(
            "nomisCSIPReportId" to it.nomisCSIPReportId,
            "offenderNo" to request.offenderNo,
            "componentsCreated" to it.components,
          ),
        )
      }
  }

  private fun findLatestBooking(offenderNo: String): OffenderBooking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
    ?: throw NotFoundException("Prisoner $offenderNo not found or has no bookings")

  fun getCSIPs(offenderNo: String): PrisonerCSIPsResponse = offenderRepository.findByNomsId(offenderNo).takeIf { it.isNotEmpty() }
    ?.let {
      PrisonerCSIPsResponse(
        csipRepository.findAllByOffenderBookingOffenderNomsId(offenderNo).map { it.toCSIPResponse() },
      )
    } ?: throw NotFoundException("Prisoner with offender no $offenderNo not found with any csips")

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

  fun getCSIPIdsForBooking(bookingId: Long): List<CSIPIdResponse> = offenderBookingRepository.findByIdOrNull(bookingId)
    ?.let { csipRepository.findIdsByOffenderBookingBookingId(bookingId).map { csipId -> CSIPIdResponse(csipId) } }
    ?: throw NotFoundException("Prisoner with booking $bookingId not found")

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
  ): Page<Long> = if (fromDate == null && toDate == null) {
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

  private fun UpsertCSIPRequest.toCreateCSIPReport(): CSIPReport {
    val latestBooking = findLatestBooking(offenderNo)
    return CSIPReport(
      offenderBooking = latestBooking,
      originalAgencyId = prisonCodeWhenRecorded,
      rootOffender = latestBooking.rootOffender,

      incidentDate = incidentDate,
      incidentTime = incidentTime?.atDate(incidentDate),

      type = lookupIncidentType(typeCode),
      location = lookupLocation(locationCode),
      areaOfWork = lookupAreaOfWork(areaOfWorkCode),

      reportedBy = reportedBy,
      reportedDate = reportedDate,
      logNumber = logNumber,
      proActiveReferral = proActiveReferral,
      staffAssaulted = staffAssaulted,
      staffAssaultedName = staffAssaultedName,
    ).apply {
      addNonMandatoryFields(this@toCreateCSIPReport)
    }
  }

  private fun updateCSIPReport(request: UpsertCSIPRequest): CSIPReport = csipRepository.findByIdOrNull(request.id!!)?.apply {
    incidentDate = request.incidentDate
    incidentTime = request.incidentTime?.atDate(incidentDate)

    type = lookupIncidentType(request.typeCode)
    location = lookupLocation(request.locationCode)
    areaOfWork = lookupAreaOfWork(request.areaOfWorkCode)

    reportedBy = request.reportedBy
    reportedDate = request.reportedDate
    logNumber = request.logNumber
    proActiveReferral = request.proActiveReferral
    staffAssaulted = request.staffAssaulted
    staffAssaultedName = request.staffAssaultedName
    addNonMandatoryFields(request)
  } ?: throw NotFoundException("CSIP Report with id=${request.id} does not exist")

  private fun CSIPReport.addNonMandatoryFields(currentRequest: UpsertCSIPRequest) {
    // Release date is not added as part of this request as it is only set when the
    // oracle form loads in the Referral continued screen
    currentRequest.reportDetailRequest?.let { reportDetailRequest ->
      involvement = reportDetailRequest.involvementCode?.let { lookupInvolvement(it) }
      concernDescription = reportDetailRequest.concern?.truncateToUtf8Length(4000)
      knownReasons = reportDetailRequest.knownReasons?.truncateToUtf8Length(4000)
      otherInformation = reportDetailRequest.otherInformation?.truncateToUtf8Length(4000)
      saferCustodyTeamInformed = reportDetailRequest.saferCustodyTeamInformed
      referralComplete = reportDetailRequest.referralComplete
      referralCompletedBy = findByStaffUserNameOrNullIfNotExists(reportDetailRequest.referralCompletedBy)
      referralCompletedDate = reportDetailRequest.referralCompletedDate
      if (reportDetailRequest.factors.isNotEmpty()) {
        addOrUpdateFactors(reportDetailRequest.factors)
      }
    }
    currentRequest.saferCustodyScreening?.let { saferCustodyScreening ->
      outcome = lookupOutcome(saferCustodyScreening.scsOutcomeCode)
      reasonForDecision = saferCustodyScreening.reasonForDecision?.truncateToUtf8Length(4000)
      outcomeCreateUsername = saferCustodyScreening.recordedBy
      outcomeCreateDate = saferCustodyScreening.recordedDate
    }

    currentRequest.investigation?.let { investigationRequest ->
      staffInvolved = investigationRequest.staffInvolved
      evidenceSecured = investigationRequest.evidenceSecured?.truncateToUtf8Length(4000)
      reasonOccurred = investigationRequest.reasonOccurred?.truncateToUtf8Length(4000)
      usualBehaviour = investigationRequest.usualBehaviour?.truncateToUtf8Length(4000)
      trigger = investigationRequest.trigger?.truncateToUtf8Length(4000)
      protectiveFactors = investigationRequest.protectiveFactors?.truncateToUtf8Length(4000)
      investigationRequest.interviews?.let {
        addOrUpdateInterviews(investigationRequest.interviews)
      }
    }
    currentRequest.decision?.let { updateDecisionData(currentRequest.decision) }

    caseManager = currentRequest.caseManager
    reasonForPlan = currentRequest.planReason
    firstCaseReviewDate = currentRequest.firstCaseReviewDate
    if (!currentRequest.plans.isNullOrEmpty()) {
      addOrUpdatePlans(planRequests = currentRequest.plans)
    }
    if (!currentRequest.reviews.isNullOrEmpty()) {
      addOrUpdateReviews(reviewRequests = currentRequest.reviews)
    }
  }

  private fun CSIPReport.addOrUpdateFactors(factorRequests: List<CSIPFactorRequest>) {
    val newFactorList: MutableList<CSIPFactor> = mutableListOf()
    factorRequests.forEach { factorRequest ->
      // Factor request has an id so find the equivalent in the report
      factorRequest.id?.let {
        factors.find { it.id == factorRequest.id }
          ?.also {
            it.type = lookupFactorType(factorRequest.typeCode)
            it.comment = factorRequest.comment?.truncateToUtf8Length(4000)
            newFactorList.add(it)
          }
          ?: throw BadDataException("Attempting to update csip report $id with a csip factor id (${factorRequest.id}) that does not exist")
      } ?: apply {
        val factor = CSIPFactor(
          csipReport = this,
          type = lookupFactorType(factorRequest.typeCode),
          comment = factorRequest.comment?.truncateToUtf8Length(4000),
        )
        newFactorList.add(factor)
      }
    }
    factors.clear()
    factors.addAll(newFactorList)
  }

  private fun CSIPReport.addOrUpdatePlans(planRequests: List<PlanRequest>) {
    val newPlanList: MutableList<CSIPPlan> = mutableListOf()
    planRequests.forEach { request ->
      newPlanList.add(
        request.id?.let {
          plans.find { it.id == request.id }
            ?.apply {
              identifiedNeed = request.identifiedNeed.truncateToUtf8Length(1000)
              intervention = request.intervention.truncateToUtf8Length(4000)
              progression = request.progression?.truncateToUtf8Length(4000)
              referredBy = request.referredBy
              targetDate = request.targetDate
              closedDate = request.closedDate
            }
            ?: throw BadDataException("Attempting to update csip report $id with a csip plan id (${request.id}) that does not exist")
        } ?: CSIPPlan(
          csipReport = this,
          identifiedNeed = request.identifiedNeed.truncateToUtf8Length(1000),
          intervention = request.intervention.truncateToUtf8Length(4000),
          progression = request.progression?.truncateToUtf8Length(4000),
          referredBy = request.referredBy,
          targetDate = request.targetDate,
          closedDate = request.closedDate,
        ),
      )
    }
    plans.clear()
    plans.addAll(newPlanList)
  }

  private fun CSIPReport.addOrUpdateReviews(reviewRequests: List<ReviewRequest>) {
    val newReviewList: MutableList<CSIPReview> = mutableListOf()
    reviewRequests.forEach { request ->
      newReviewList.add(
        if (request.id != null) {
          reviews.find { it.id == request.id }
            ?.apply {
              remainOnCSIP = request.remainOnCSIP
              csipUpdated = request.csipUpdated
              caseNote = request.caseNote
              closeCSIP = request.closeCSIP
              peopleInformed = request.peopleInformed
              summary = request.summary?.truncateToUtf8Length(4000)
              nextReviewDate = request.nextReviewDate
              closeDate = request.closeDate
              reviewSequence = request.reviewSequence
            }
            ?: throw BadDataException("Attempting to update csip report $id with a csip review id (${request.id}) that does not exist")
        } else {
          CSIPReview(
            csipReport = this,
            remainOnCSIP = request.remainOnCSIP,
            csipUpdated = request.csipUpdated,
            caseNote = request.caseNote,
            closeCSIP = request.closeCSIP,
            peopleInformed = request.peopleInformed,
            summary = request.summary?.truncateToUtf8Length(4000),
            nextReviewDate = request.nextReviewDate,
            closeDate = request.closeDate,
            recordedDate = request.recordedDate,
            recordedUser = request.recordedBy,
            reviewSequence = request.reviewSequence,
          )
        }.apply {
          if (!request.attendees.isNullOrEmpty()) {
            addOrUpdateAttendees(request.attendees)
          }
        },
      )
    }
    reviews.clear()
    reviews.addAll(newReviewList)
  }

  private fun CSIPReport.addOrUpdateInterviews(requests: List<InterviewDetailRequest>) {
    val newInterviewList: MutableList<CSIPInterview> = mutableListOf()
    requests.forEach { request ->
      newInterviewList.add(
        // Interview request has an id so find the equivalent in the report
        request.id?.let {
          interviews.find { it.id == request.id }
            ?. apply {
              interviewee = request.interviewee
              interviewDate = request.date
              role = lookupInterviewRoleType(request.roleCode)
              comments = request.comments?.truncateToUtf8Length(4000)
            }
            ?: throw BadDataException("Attempting to update csip report $id with a csip interview id (${request.id}) that does not exist")
        } ?: CSIPInterview(
          csipReport = this,
          interviewee = request.interviewee,
          interviewDate = request.date,
          role = lookupInterviewRoleType(request.roleCode),
          comments = request.comments?.truncateToUtf8Length(4000),
        ),
      )
    }
    interviews.clear()
    interviews.addAll(newInterviewList)
  }

  private fun CSIPReview.addOrUpdateAttendees(attendeeRequests: List<AttendeeRequest>) {
    val newAttendeeList: MutableList<CSIPAttendee> = mutableListOf()
    attendeeRequests.forEach { request ->
      newAttendeeList.add(
        request.id?.let {
          attendees.find { it.id == request.id }
            ?.apply {
              name = request.name
              role = request.role
              attended = request.attended
              contribution = request.contribution
            }
            ?: throw BadDataException("Attempting to update csip report ${csipReport.id} with a csip attendee id (${request.id}) that does not exist")
        }
          ?: CSIPAttendee(
            csipReview = this,
            name = request.name,
            role = request.role,
            attended = request.attended,
            contribution = request.contribution,
          ),
      )
    }
    attendees.clear()
    attendees.addAll(newAttendeeList)
  }

  private fun CSIPReport.updateDecisionData(decision: DecisionRequest) {
    conclusion = decision.conclusion?.truncateToUtf8Length(4000)
    decisionOutcome = decision.decisionOutcomeCode?.let { lookupOutcome(it) }
    signedOffRole = decision.signedOffRoleCode?.let { lookupSignedOffRole(it) }
    recordedBy = decision.recordedBy
    recordedDate = decision.recordedDate
    nextSteps = decision.nextSteps?.truncateToUtf8Length(4000)
    otherDetails = decision.otherDetails?.truncateToUtf8Length(4000)
    openCSIPAlert = decision.actions.openCSIPAlert
    nonAssociationsUpdated = decision.actions.nonAssociationsUpdated
    observationBook = decision.actions.observationBook
    unitOrCellMove = decision.actions.unitOrCellMove
    csraOrRsraReview = decision.actions.csraOrRsraReview
    serviceReferral = decision.actions.serviceReferral
    simReferral = decision.actions.simReferral
  }

  fun lookupAreaOfWork(code: String) = areaOfWorkRepository.findByIdOrNull(CSIPAreaOfWork.pk(code))
    ?: throw BadDataException("Area of work type $code not found")
  fun lookupIncidentType(code: String) = typeRepository.findByIdOrNull(CSIPIncidentType.pk(code))
    ?: throw BadDataException("Incident type $code not found")
  fun lookupInvolvement(code: String) = involvementRepository.findByIdOrNull(CSIPInvolvement.pk(code))
    ?: throw BadDataException("Involvement type $code not found")
  fun lookupInterviewRoleType(code: String) = interviewRoleRepository.findByIdOrNull(CSIPInterviewRole.pk(code))
    ?: throw BadDataException("Interview role type $code not found")
  fun lookupLocation(code: String) = locationRepository.findByIdOrNull(CSIPIncidentLocation.pk(code))
    ?: throw BadDataException("Location type $code not found")
  fun lookupOutcome(code: String) = outcomeRepository.findByIdOrNull(CSIPOutcome.pk(code))
    ?: throw BadDataException("Outcome type $code not found")
  fun lookupSignedOffRole(code: String) = signedOffRoleRepository.findByIdOrNull(CSIPSignedOffRole.pk(code))
    ?: throw BadDataException("Signed off role type $code not found")
  fun lookupFactorType(code: String) = factorRepository.findByIdOrNull(CSIPFactorType.pk(code))
    ?: throw BadDataException("Factor type $code not found")

  fun findByStaffUserNameOrNullIfNotExists(staffUserName: String?): String? = staffUserName?.let { staffUserAccountRepository.findByUsername(staffUserName)?.username }
}

fun CSIPReport.identifyNewComponents(request: UpsertCSIPRequest): List<CSIPComponent> = identifyNewChildComponents(request.reportDetailRequest?.factors, factors, FACTOR) +
  identifyNewChildComponents(request.plans, plans, PLAN) +
  identifyNewChildComponents(request.investigation?.interviews, interviews, INTERVIEW) +
  identifyNewReviewsAndAttendees(request)

fun CSIPReport.identifyNewReviewsAndAttendees(request: UpsertCSIPRequest): List<CSIPComponent> = request.reviews?.let {
  identifyNewChildComponents(request.reviews, reviews, REVIEW) +
    it.zip(reviews) { reviewReq, existingReview ->
      identifyNewChildComponents(reviewReq.attendees, existingReview.attendees, ATTENDEE)
    }.flatten()
} ?: listOf()

fun identifyNewChildComponents(requestList: List<CSIPChildRequest>?, existingList: List<CSIPChild>, component: CSIPComponent.Component): List<CSIPComponent> {
  val componentsList = mutableListOf<CSIPComponent>()
  requestList?.onEachIndexed { index, requestItem ->
    requestItem.id ?: componentsList.add(CSIPComponent(component, existingList[index].id, requestItem.dpsId))
  }
  return componentsList
}
