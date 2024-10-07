package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.DocumentService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.trackEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPAreaOfWork
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPFactor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPFactorType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPIncidentLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPIncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPInvolvement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPSignedOffRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReportRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
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
  val involvementRepository: ReferenceCodeRepository<CSIPInvolvement>,
  val locationRepository: ReferenceCodeRepository<CSIPIncidentLocation>,
  val signedOffRoleRepository: ReferenceCodeRepository<CSIPSignedOffRole>,
  val outcomeRepository: ReferenceCodeRepository<CSIPOutcome>,
  val typeRepository: ReferenceCodeRepository<CSIPIncidentType>,
  val factorRepository: ReferenceCodeRepository<CSIPFactorType>,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val csipTemplates = listOf("CSIPA1_HMP", "CSIPA1_FNP", "CSIPA2_HMP", "CSIPA2_FNP", "CSIPA3_HMP", "CSIPA3_FNP")
  }

  fun upsertCSIP(request: UpsertCSIPRequest): UpsertCSIPResponse {
    log.debug("Received upsert request {}", request)
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
          created = toCreate,
        )
      }.also {
        telemetryClient.trackEvent(
          "csip-${if (toCreate) "created" else "updated"}",
          mapOf(
            "nomisCSIPReportId" to it.nomisCSIPReportId,
            "offenderNo" to request.offenderNo,
            "upsert" to if (toCreate) "created" else "updated",
          ),
        )
      }
  }

  private fun findLatestBooking(offenderNo: String): OffenderBooking =
    offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Prisoner $offenderNo not found or has no bookings")

  fun getCSIPs(offenderNo: String): PrisonerCSIPsResponse =
    offenderRepository.findByNomsId(offenderNo).takeIf { it.isNotEmpty() }
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
      createUsername = auditDetails.createUsername,
    ).apply {
      addNonMandatoryFields(this@toCreateCSIPReport)
    }
  }

  private fun CSIPReport.addNonMandatoryFields(currentRequest: UpsertCSIPRequest) {
    // TODO Add release date -> Confirmed Release Date (from External Movements)
    // releaseDate = latestBooking.releaseDetail.releaseDate,
    currentRequest.reportDetailRequest?.let { reportDetailRequest ->
      involvement = reportDetailRequest.involvementCode?.let { lookupInvolvement(it) }
      concernDescription = reportDetailRequest.concern
      knownReasons = reportDetailRequest.knownReasons
      otherInformation = reportDetailRequest.otherInformation
      saferCustodyTeamInformed = reportDetailRequest.saferCustodyTeamInformed
      referralComplete = reportDetailRequest.referralComplete
      referralCompletedBy = reportDetailRequest.referralCompletedBy
      referralCompletedDate = reportDetailRequest.referralCompletedDate
      if (reportDetailRequest.factors.isNotEmpty()) {
        addOrInsertFactor(request = currentRequest)
      }
    }

    currentRequest.saferCustodyScreening?.let { saferCustodyScreening ->
      outcome = lookupOutcome(saferCustodyScreening.scsOutcomeCode)
      reasonForDecision = saferCustodyScreening.reasonForDecision
      outcomeCreateUsername = saferCustodyScreening.recordedBy
      outcomeCreateDate = saferCustodyScreening.recordedDate
    }

    currentRequest.investigation?.let { investigation ->
      staffInvolved = investigation.staffInvolved
      evidenceSecured = investigation.evidenceSecured
      reasonOccurred = investigation.reasonOccurred
      usualBehaviour = investigation.usualBehaviour
      trigger = investigation.trigger
      protectiveFactors = investigation.protectiveFactors
      // TODO interviews = mutableListOf()
    }
    currentRequest.decision?.let { setDecisionData(currentRequest.decision) }

    caseManager = currentRequest.caseManager
    reasonForPlan = currentRequest.planReason
    firstCaseReviewDate = currentRequest.firstCaseReviewDate
    // TODO plans = mutableListOf()
    // TODO reviews = mutableListOf() and attendees

    lastModifiedUsername = currentRequest.auditDetails.modifyUsername
    lastModifiedDateTime = currentRequest.auditDetails.modifyDatetime
  }

  private fun CSIPReport.addOrInsertFactor(request: UpsertCSIPRequest) {
    request.reportDetailRequest?.factors?.forEach { factorRequest ->
      factorRequest.id?.let {
        factors.find { it.id == factorRequest.id }
          ?.also {
            it.type = lookupFactorType(factorRequest.typeCode)
            it.comment = factorRequest.comment
            it.createUsername = factorRequest.auditDetails.createUsername
          }
          ?: {
            throw BadDataException("Attempting to update csip report $id with a csip factor id (${factorRequest.id}) that does not exist")
          }
      } ?: apply {
        val factor = CSIPFactor(
          csipReport = this,
          type = lookupFactorType(factorRequest.typeCode),
          comment = factorRequest.comment,
          createUsername = factorRequest.auditDetails.createUsername,
        )

        factors.add(factor)
      }
    }
  }

  private fun CSIPReport.setDecisionData(decision: DecisionRequest) {
    conclusion = decision.conclusion
    decisionOutcome = decision.decisionOutcomeCode?.let { lookupOutcome(it) }
    signedOffRole = decision.signedOffRoleCode?.let { lookupSignedOffRole(it) }
    recordedBy = decision.recordedBy
    recordedDate = decision.recordedDate
    nextSteps = decision.nextSteps
    otherDetails = decision.otherDetails
    openCSIPAlert = decision.actions.openCSIPAlert
    nonAssociationsUpdated = decision.actions.nonAssociationsUpdated
    observationBook = decision.actions.observationBook
    unitOrCellMove = decision.actions.unitOrCellMove
    csraOrRsraReview = decision.actions.csraOrRsraReview
    serviceReferral = decision.actions.serviceReferral
    simReferral = decision.actions.simReferral
  }

  private fun updateCSIPReport(request: UpsertCSIPRequest): CSIPReport =
    csipRepository.findByIdOrNull(request.id!!)?.apply {
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
    } ?: throw NotFoundException("CSIP Report with id=${request.id} does not exist")

  fun lookupAreaOfWork(code: String) =
    areaOfWorkRepository.findByIdOrNull(CSIPAreaOfWork.pk(code)) ?: throw BadDataException("Area of work type $code not found")
  fun lookupIncidentType(code: String) =
    typeRepository.findByIdOrNull(CSIPIncidentType.pk(code)) ?: throw BadDataException("Incident type $code not found")
  fun lookupInvolvement(code: String) =
    involvementRepository.findByIdOrNull(CSIPInvolvement.pk(code)) ?: throw BadDataException("Involvement type $code not found")
  fun lookupLocation(code: String) =
    locationRepository.findByIdOrNull(CSIPIncidentLocation.pk(code)) ?: throw BadDataException("Location type $code not found")
  fun lookupOutcome(code: String) = outcomeRepository.findByIdOrNull(CSIPOutcome.pk(code))
    ?: throw BadDataException("Outcome type $code not found")
  fun lookupSignedOffRole(code: String) = signedOffRoleRepository.findByIdOrNull(CSIPSignedOffRole.pk(code))
    ?: throw BadDataException("Signed off role type $code not found")
  fun lookupFactorType(code: String) = factorRepository.findByIdOrNull(CSIPFactorType.pk(code))
    ?: throw BadDataException("Factor type $code not found")
}
