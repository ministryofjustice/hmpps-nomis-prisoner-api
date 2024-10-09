package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPAreaOfWork
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPFactor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPIncidentLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPIncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPInterview
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPInvolvement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPPlan
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReview
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPSignedOffRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReportRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DslMarker
annotation class CSIPReportDslMarker

@DslMarker
annotation class CSIPInvestigationDslMarker

@DslMarker
annotation class CSIPSaferCustodyScreeningDslMarker

@DslMarker
annotation class CSIPDecisionDslMarker

@NomisDataDslMarker
interface CSIPReportDsl {

  @CSIPFactorDslMarker
  fun factor(
    factor: String = "BUL",
    comment: String? = null,
    dsl: CSIPFactorDsl.() -> Unit = {},
  ): CSIPFactor

  @CSIPSaferCustodyScreeningDslMarker
  fun scs(
    outcome: String = "ACC",
    reasonForDecision: String = "Further help needed",
    outcomeCreateUsername: String = "FRED.JAMES",
    outcomeCreateDate: LocalDate = LocalDate.now(),
  )

  @CSIPInvestigationDslMarker
  fun investigation(
    staffInvolved: String? = null,
    evidenceSecured: String? = null,
    reasonOccurred: String? = null,
    usualBehaviour: String? = null,
    trigger: String? = null,
    protectiveFactors: String? = null,
  )

  @CSIPInterviewDslMarker
  fun interview(
    interviewee: String = "Jim the Interviewee",
    interviewDate: LocalDate = LocalDate.now(),
    role: String = "WITNESS",
    comment: String? = null,
    dsl: CSIPInterviewDsl.() -> Unit = {},
  ): CSIPInterview

  @CSIPPlanDslMarker
  fun plan(
    identifiedNeed: String = "They need help",
    intervention: String = "Support their work",
    progression: String? = null,
    referredBy: String = "Fred Bloggs",
    targetDate: LocalDate = LocalDate.now(),
    dsl: CSIPPlanDsl.() -> Unit = {},
  ): CSIPPlan

  @CSIPReviewDslMarker
  fun review(
    remainOnCSIP: Boolean = true,
    csipUpdated: Boolean = true,
    caseNote: Boolean = false,
    closeCSIP: Boolean = false,
    peopleInformed: Boolean = false,
    summary: String? = "More help needed",
    nextReview: LocalDate? = LocalDate.parse("2024-08-01"),
    closeDate: LocalDate? = null,
    recordedBy: String? = "FRED.JAMES",
    dsl: CSIPReviewDsl.() -> Unit = {},
  ): CSIPReview

  @CSIPDecisionDslMarker
  fun decision(
    conclusion: String = "The end result",
    decisionOutcome: String = "NFA",
    signedOffBy: String = "CUSTMAN",
    recordedBy: String = "FRED.JAMES",
    recordedDate: LocalDate = LocalDate.now(),
    nextSteps: String = "provide help",
    otherDetails: String = "Support and assistance needed",
    openCSIPAlert: Boolean = true,
    nonAssociationsUpdated: Boolean = false,
    observationBook: Boolean = true,
    unitOrCellMove: Boolean = true,
    csraOrRsraReview: Boolean = false,
    serviceReferral: Boolean = true,
    simReferral: Boolean = false,
  )
}

@Component
class CSIPReportBuilderFactory(
  private val repository: CSIPReportBuilderRepository,
  private val csipInterviewBuilderFactory: CSIPInterviewBuilderFactory,
  private val csipPlanBuilderFactory: CSIPPlanBuilderFactory,
  private val csipReviewBuilderFactory: CSIPReviewBuilderFactory,
  private val csipFactorBuilderFactory: CSIPFactorBuilderFactory,

) {
  fun builder(): CSIPReportBuilder {
    return CSIPReportBuilder(
      repository,
      csipInterviewBuilderFactory,
      csipPlanBuilderFactory,
      csipReviewBuilderFactory,
      csipFactorBuilderFactory,
    )
  }
}

@Component
class CSIPReportBuilderRepository(
  private val repository: CSIPReportRepository,
  val typeRepository: ReferenceCodeRepository<CSIPIncidentType>,
  val locationRepository: ReferenceCodeRepository<CSIPIncidentLocation>,
  val areaOfWorkRepository: ReferenceCodeRepository<CSIPAreaOfWork>,
  val outcomeRepository: ReferenceCodeRepository<CSIPOutcome>,
  val involvementRepository: ReferenceCodeRepository<CSIPInvolvement>,
  val signedOffRoleRepository: ReferenceCodeRepository<CSIPSignedOffRole>,
  // TODO Check data in preprod if decisionOutcome and outcome the same
  val decisionOutcomeRepository: ReferenceCodeRepository<CSIPOutcome>,
) {
  fun save(csipReport: CSIPReport): CSIPReport = repository.save(csipReport)
  fun lookupType(code: String) = typeRepository.findByIdOrNull(CSIPIncidentType.pk(code))!!
  fun lookupLocation(code: String) = locationRepository.findByIdOrNull(CSIPIncidentLocation.pk(code))!!
  fun lookupAreaOfWork(code: String) = areaOfWorkRepository.findByIdOrNull(CSIPAreaOfWork.pk(code))!!
  fun lookupOutcome(code: String) = outcomeRepository.findByIdOrNull(CSIPOutcome.pk(code))!!
  fun lookupInvolvement(code: String) = involvementRepository.findByIdOrNull(CSIPInvolvement.pk(code))!!
  fun lookupSignedOffRole(code: String) = signedOffRoleRepository.findByIdOrNull(CSIPSignedOffRole.pk(code))!!
  fun lookupDecisionOutcome(code: String) = decisionOutcomeRepository.findByIdOrNull(CSIPOutcome.pk(code))!!
}

class CSIPReportBuilder(
  private val repository: CSIPReportBuilderRepository,
  private val csipInterviewBuilderFactory: CSIPInterviewBuilderFactory,
  private val csipPlanBuilderFactory: CSIPPlanBuilderFactory,
  private val csipReviewBuilderFactory: CSIPReviewBuilderFactory,
  private val csipFactorBuilderFactory: CSIPFactorBuilderFactory,

) : CSIPReportDsl {
  private lateinit var csipReport: CSIPReport

  fun build(
    offenderBooking: OffenderBooking,
    type: String,
    location: String,
    areaOfWork: String,
    reportedBy: String,
    incidentDate: LocalDate,
    incidentTime: LocalTime?,
    staffAssaulted: Boolean,
    staffAssaultedName: String?,
    releaseDate: LocalDate?,
    involvement: String?,
    concern: String?,
    knownReasons: String?,
    otherInformation: String?,
    referralComplete: Boolean,
    referralCompletedBy: String?,
    referralCompletedDate: LocalDate?,
    caseManager: String?,
    planReason: String?,
    firstCaseReviewDate: LocalDate?,
    logNumber: String?,
    createUsername: String,
  ): CSIPReport =
    CSIPReport(
      offenderBooking = offenderBooking,
      rootOffender = offenderBooking.rootOffender,
      originalAgencyId = offenderBooking.location?.id,
      type = repository.lookupType(type),
      location = repository.lookupLocation(location),
      areaOfWork = repository.lookupAreaOfWork(areaOfWork),
      reportedBy = reportedBy,
      incidentDate = incidentDate,
      incidentTime = incidentTime?.let { LocalDateTime.of(LocalDate.now(), incidentTime) },
      staffAssaulted = staffAssaulted,
      staffAssaultedName = staffAssaultedName,
      releaseDate = releaseDate,
      involvement = involvement?.let { repository.lookupInvolvement(involvement) },
      concernDescription = concern,
      knownReasons = knownReasons,
      otherInformation = otherInformation,
      referralComplete = referralComplete,
      referralCompletedBy = referralCompletedBy,
      referralCompletedDate = referralCompletedDate,
      caseManager = caseManager,
      reasonForPlan = planReason,
      firstCaseReviewDate = firstCaseReviewDate,
      logNumber = logNumber,
    )
      .let { repository.save(it) }
      .also { csipReport = it }

  override fun factor(
    type: String,
    comment: String?,
    dsl: CSIPFactorDsl.() -> Unit,
  ): CSIPFactor = csipFactorBuilderFactory.builder()
    .let { builder ->
      builder.build(
        csipReport = csipReport,
        type = type,
        comment = comment,
      )
        .also { csipReport.factors += it }
        .also { builder.apply(dsl) }
    }

  override fun scs(
    outcome: String,
    reasonForDecision: String,
    outcomeCreateUsername: String,
    outcomeCreateDate: LocalDate,
  ) {
    csipReport.outcome = repository.lookupOutcome(outcome)
    csipReport.reasonForDecision = reasonForDecision
    csipReport.outcomeCreateUsername = outcomeCreateUsername
    csipReport.outcomeCreateDate = outcomeCreateDate

    repository.save(csipReport)
  }

  override fun investigation(
    staffInvolved: String?,
    evidenceSecured: String?,
    reasonOccurred: String?,
    usualBehaviour: String?,
    trigger: String?,
    protectiveFactors: String?,
  ) {
    csipReport.staffInvolved = staffInvolved
    csipReport.evidenceSecured = evidenceSecured
    csipReport.reasonOccurred = reasonOccurred
    csipReport.usualBehaviour = usualBehaviour
    csipReport.trigger = trigger
    csipReport.protectiveFactors = protectiveFactors

    repository.save(csipReport)
  }

  override fun interview(
    interviewee: String,
    interviewDate: LocalDate,
    role: String,
    comments: String?,
    dsl: CSIPInterviewDsl.() -> Unit,
  ): CSIPInterview = csipInterviewBuilderFactory.builder()
    .let { builder ->
      builder.build(
        csipReport = csipReport,
        interviewee = interviewee,
        interviewDate = interviewDate,
        role = role,
        comments = comments,
      )
        .also { csipReport.interviews += it }
        .also { builder.apply(dsl) }
    }

  override fun plan(
    identifiedNeed: String,
    intervention: String,
    progression: String?,
    referredBy: String,
    targetDate: LocalDate,
    dsl: CSIPPlanDsl.() -> Unit,
  ): CSIPPlan = csipPlanBuilderFactory.builder()
    .let { builder ->
      builder.build(
        csipReport = csipReport,
        identifiedNeed = identifiedNeed,
        intervention = intervention,
        progression = progression,
        referredBy = referredBy,
        targetDate = targetDate,
      )
        .also { csipReport.plans += it }
        .also { builder.apply(dsl) }
    }

  override fun review(
    remainOnCSIP: Boolean,
    csipUpdated: Boolean,
    caseNote: Boolean,
    closeCSIP: Boolean,
    peopleInformed: Boolean,
    summary: String?,
    nextReview: LocalDate?,
    closeDate: LocalDate?,
    recordedBy: String?,
    dsl: CSIPReviewDsl.() -> Unit,
  ): CSIPReview = csipReviewBuilderFactory.builder()
    .let { builder ->
      builder.build(
        csipReport = csipReport,
        reviewSequence = csipReport.reviews.size + 1,
        remainOnCSIP = remainOnCSIP,
        csipUpdated = csipUpdated,
        caseNote = caseNote,
        closeCSIP = closeCSIP,
        peopleInformed = peopleInformed,
        summary = summary,
        nextReviewDate = nextReview,
        closeDate = closeDate,
        recordedBy = recordedBy,
      )
        .also { csipReport.reviews += it }
        .also { builder.apply(dsl) }
    }

  override fun decision(
    conclusion: String,
    decisionOutcome: String,
    signedOffRole: String,
    recordedBy: String,
    recordedDate: LocalDate,
    nextSteps: String,
    otherDetails: String,
    openCSIPAlert: Boolean,
    nonAssociationsUpdated: Boolean,
    observationBook: Boolean,
    unitOrCellMove: Boolean,
    csraOrRsraReview: Boolean,
    serviceReferral: Boolean,
    simReferral: Boolean,
  ) {
    csipReport.conclusion = conclusion
    csipReport.decisionOutcome = repository.lookupDecisionOutcome(decisionOutcome)
    csipReport.signedOffRole = repository.lookupSignedOffRole(signedOffRole)
    csipReport.recordedBy = recordedBy
    csipReport.recordedDate = recordedDate
    csipReport.nextSteps = nextSteps
    csipReport.otherDetails = otherDetails

    csipReport.openCSIPAlert = openCSIPAlert
    csipReport.nonAssociationsUpdated = nonAssociationsUpdated
    csipReport.observationBook = observationBook
    csipReport.unitOrCellMove = unitOrCellMove
    csipReport.csraOrRsraReview = csraOrRsraReview
    csipReport.serviceReferral = serviceReferral
    csipReport.simReferral = simReferral
    repository.save(csipReport)
  }
}
