package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ExternalService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalDateTime

@Component
@Transactional
class NomisDataBuilder(
  private val programServiceBuilderFactory: ProgramServiceBuilderFactory? = ProgramServiceBuilderFactory(),
  private val offenderBuilderFactory: OffenderBuilderFactory? = null,
  private val staffBuilderFactory: StaffBuilderFactory? = null,
  private val adjudicationIncidentBuilderFactory: AdjudicationIncidentBuilderFactory? = null,
  private val nonAssociationBuilderFactory: NonAssociationBuilderFactory? = null,
  private val externalServiceBuilderFactory: ExternalServiceBuilderFactory? = null,
  private val courtCaseBuilderFactory: CourtCaseBuilderFactory? = null,
) {
  fun build(dsl: NomisData.() -> Unit) = NomisData(
    programServiceBuilderFactory,
    offenderBuilderFactory,
    staffBuilderFactory,
    adjudicationIncidentBuilderFactory,
    nonAssociationBuilderFactory,
    externalServiceBuilderFactory,
    courtCaseBuilderFactory,
  ).apply(dsl)
}

class NomisData(
  private val programServiceBuilderFactory: ProgramServiceBuilderFactory? = null,
  private val offenderBuilderFactory: OffenderBuilderFactory? = null,
  private val staffBuilderFactory: StaffBuilderFactory? = null,
  private val adjudicationIncidentBuilderFactory: AdjudicationIncidentBuilderFactory? = null,
  private val nonAssociationBuilderFactory: NonAssociationBuilderFactory? = null,
  private val externalServiceBuilderFactory: ExternalServiceBuilderFactory? = null,
  private val courtCaseBuilderFactory: CourtCaseBuilderFactory? = null,
) : NomisDataDsl {
  @StaffDslMarker
  override fun staff(firstName: String, lastName: String, dsl: StaffDsl.() -> Unit): Staff =
    staffBuilderFactory!!.builder()
      .let { builder ->
        builder.build(lastName, firstName)
          .also {
            builder.apply(dsl)
          }
      }

  @AdjudicationIncidentDslMarker
  override fun adjudicationIncident(
    whenCreated: LocalDateTime,
    incidentDetails: String,
    reportedDateTime: LocalDateTime,
    reportedDate: LocalDate,
    incidentDateTime: LocalDateTime,
    incidentDate: LocalDate,
    prisonId: String,
    agencyInternalLocationId: Long,
    reportingStaff: Staff,
    dsl: AdjudicationIncidentDsl.() -> Unit,
  ): AdjudicationIncident = adjudicationIncidentBuilderFactory!!.builder()
    .let { builder ->
      builder.build(
        whenCreated = whenCreated,
        incidentDetails = incidentDetails,
        reportedDateTime = reportedDateTime,
        reportedDate = reportedDate,
        incidentDateTime = incidentDateTime,
        incidentDate = incidentDate,
        prisonId = prisonId,
        agencyInternalLocationId = agencyInternalLocationId,
        reportingStaff = reportingStaff,
      )
        .also {
          builder.apply(dsl)
        }
    }

  @OffenderDslMarker
  override fun offender(
    nomsId: String,
    lastName: String,
    firstName: String,
    birthDate: LocalDate,
    genderCode: String,
    dsl: OffenderDsl.() -> Unit,
  ): Offender =
    offenderBuilderFactory!!.builder()
      .let { builder ->
        builder.build(nomsId, lastName, firstName, birthDate, genderCode)
          .also {
            builder.apply(dsl)
          }
      }

  @ProgramServiceDslMarker
  override fun programService(
    programCode: String,
    programId: Long,
    description: String,
    active: Boolean,
    dsl: ProgramServiceDsl.() -> Unit,
  ): ProgramService =
    programServiceBuilderFactory!!.builder()
      .let { builder ->
        builder.build(programCode, programId, description, active)
          .also {
            builder.apply(dsl)
          }
      }

  @NonAssociationDslMarker
  override fun nonAssociation(
    offender1: Offender,
    offender2: Offender,
    nonAssociationReason: String,
    recipNonAssociationReason: String,
    dsl: NonAssociationDsl.() -> Unit,
  ): OffenderNonAssociation =
    nonAssociationBuilderFactory!!.builder()
      .let { builder ->
        builder.build(
          offender1.id,
          offender2.id,
          offender1.latestBooking(),
          offender2.latestBooking(),
          nonAssociationReason,
          recipNonAssociationReason,
        )
          .also {
            builder.apply(dsl)
          }
      }

  @ExternalServiceDslMarker
  override fun externalService(
    serviceName: String,
    description: String,
    dsl: ExternalServiceDsl.() -> Unit,
  ): ExternalService =
    externalServiceBuilderFactory!!.builder()
      .let { builder ->
        builder.build(
          serviceName,
          description,
        )
          .also {
            builder.apply(dsl)
          }
      }

  @CourtCaseDslMarker
  override fun courtCase(
    offender: Offender,
    offenderBooking: OffenderBooking?,
    whenCreated: LocalDateTime,
    caseStatus: String,
    caseType: String,
    beginDate: LocalDate,
    caseSequence: Int,
    caseInfoNumber: String?,
    prisonId: String,
    combinedCase: CourtCase?,
    reportingStaff: Staff,
    statusUpdateStaff: Staff?,
    statusUpdateDate: LocalDate?,
    statusUpdateReason: String?,
    statusUpdateComment: String?,
    lidsCaseNumber: Int,
    lidsCaseId: Int?,
    lidsCombinedCaseId: Int?,
    dsl: CourtCaseDsl.() -> Unit,
  ): CourtCase = courtCaseBuilderFactory!!.builder()
    .let { builder ->
      builder.build(
        whenCreated = whenCreated,
        offenderBooking = offenderBooking ?: offender.latestBooking(),
        combinedCase = combinedCase,
        caseStatus = caseStatus,
        caseType = caseType,
        caseSequence = caseSequence,
        beginDate = beginDate,
        caseInfoNumber = caseInfoNumber,
        prisonId = prisonId,
        statusUpdateStaff = statusUpdateStaff,
        statusUpdateDate = statusUpdateDate,
        statusUpdateComment = statusUpdateComment,
        statusUpdateReason = statusUpdateReason,
        lidsCaseNumber = lidsCaseNumber,
        lidsCaseId = lidsCaseId,
        lidsCombinedCaseId = lidsCombinedCaseId,
      )
        .also {
          builder.apply(dsl)
        }
    }
}

@NomisDataDslMarker
interface NomisDataDsl {
  @OffenderDslMarker
  fun offender(
    nomsId: String = "A5194DY",
    lastName: String = "NTHANDA",
    firstName: String = "LEKAN",
    birthDate: LocalDate = LocalDate.of(1965, 7, 19),
    genderCode: String = "M",
    dsl: OffenderDsl.() -> Unit = {},
  ): Offender

  @ProgramServiceDslMarker
  fun programService(
    programCode: String = "INTTEST",
    programId: Long = 0,
    description: String = "test program",
    active: Boolean = true,
    dsl: ProgramServiceDsl.() -> Unit = {},
  ): ProgramService

  @StaffDslMarker
  fun staff(firstName: String = "AAYAN", lastName: String = "AHMAD", dsl: StaffDsl.() -> Unit = {}): Staff

  @AdjudicationIncidentDslMarker
  fun adjudicationIncident(
    whenCreated: LocalDateTime = LocalDateTime.now(),
    incidentDetails: String = "Big fight",
    reportedDateTime: LocalDateTime = LocalDateTime.now(),
    reportedDate: LocalDate = LocalDate.now(),
    incidentDateTime: LocalDateTime = LocalDateTime.now(),
    incidentDate: LocalDate = LocalDate.now(),
    prisonId: String = "MDI",
    agencyInternalLocationId: Long = -41,
    reportingStaff: Staff,
    dsl: AdjudicationIncidentDsl.() -> Unit = {},
  ): AdjudicationIncident

  @NonAssociationDslMarker
  fun nonAssociation(
    offender1: Offender,
    offender2: Offender,
    nonAssociationReason: String = "PER",
    recipNonAssociationReason: String = "VIC",
    dsl: NonAssociationDsl.() -> Unit = {},
  ): OffenderNonAssociation

  @ExternalServiceDslMarker
  fun externalService(
    serviceName: String,
    description: String = serviceName,
    dsl: ExternalServiceDsl.() -> Unit = {},
  ): ExternalService

  @CourtCaseDslMarker
  fun courtCase(
    offender: Offender,
    offenderBooking: OffenderBooking? = null,
    whenCreated: LocalDateTime = LocalDateTime.now(),
    caseStatus: String = "A",
    caseType: String = "A",
    beginDate: LocalDate = LocalDate.now(),
    caseSequence: Int = 1,
    caseInfoNumber: String? = "AB1",
    prisonId: String = "MDI",
    combinedCase: CourtCase? = null,
    reportingStaff: Staff,
    statusUpdateStaff: Staff? = null,
    statusUpdateDate: LocalDate? = null,
    statusUpdateReason: String? = "a reason",
    statusUpdateComment: String? = "a comment",
    lidsCaseNumber: Int = 1,
    lidsCaseId: Int? = 2,
    lidsCombinedCaseId: Int? = 3,
    dsl: CourtCaseDsl.() -> Unit,
  ): CourtCase
}

@DslMarker
annotation class NomisDataDslMarker
