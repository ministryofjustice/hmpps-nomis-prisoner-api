package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ExternalService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MergeTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
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
  private val agencyInternalLocationBuilderFactory: AgencyInternalLocationBuilderFactory? = null,
  private val courtCaseBuilderFactory: CourtCaseBuilderFactory? = null,
  private val questionnaireBuilderFactory: QuestionnaireBuilderFactory? = null,
  private val incidentBuilderFactory: IncidentBuilderFactory? = null,
  private val mergeTransactionBuilderFactory: MergeTransactionBuilderFactory? = null,
  private val csipReportBuilderFactory: CSIPReportBuilderFactory? = null,
) {
  fun build(dsl: NomisData.() -> Unit) = NomisData(
    programServiceBuilderFactory,
    offenderBuilderFactory,
    staffBuilderFactory,
    adjudicationIncidentBuilderFactory,
    nonAssociationBuilderFactory,
    externalServiceBuilderFactory,
    agencyInternalLocationBuilderFactory,
    courtCaseBuilderFactory,
    questionnaireBuilderFactory,
    incidentBuilderFactory,
    mergeTransactionBuilderFactory,
    csipReportBuilderFactory,
  ).apply(dsl)
}

class NomisData(
  private val programServiceBuilderFactory: ProgramServiceBuilderFactory? = null,
  private val offenderBuilderFactory: OffenderBuilderFactory? = null,
  private val staffBuilderFactory: StaffBuilderFactory? = null,
  private val adjudicationIncidentBuilderFactory: AdjudicationIncidentBuilderFactory? = null,
  private val nonAssociationBuilderFactory: NonAssociationBuilderFactory? = null,
  private val externalServiceBuilderFactory: ExternalServiceBuilderFactory? = null,
  private val agencyInternalLocationBuilderFactory: AgencyInternalLocationBuilderFactory? = null,
  private val courtCaseBuilderFactory: CourtCaseBuilderFactory? = null,
  private val questionnaireBuilderFactory: QuestionnaireBuilderFactory? = null,
  private val incidentBuilderFactory: IncidentBuilderFactory? = null,
  private val mergeTransactionBuilderFactory: MergeTransactionBuilderFactory? = null,
  private val csipReportBuilderFactory: CSIPReportBuilderFactory? = null,
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

  @QuestionnaireDslMarker
  override fun questionnaire(
    code: String,
    description: String,
    active: Boolean,
    listSequence: Int,
    dsl: QuestionnaireDsl.() -> Unit,
  ): Questionnaire =
    questionnaireBuilderFactory!!.builder()
      .let { builder ->
        builder.build(
          code = code,
          description = description,
          active = active,
          listSequence = listSequence,
        )
          .also {
            builder.apply(dsl)
          }
      }

  @IncidentDslMarker
  override fun incident(
    title: String,
    description: String,
    prisonId: String,
    reportingStaff: Staff,
    reportedDateTime: LocalDateTime,
    incidentDateTime: LocalDateTime,
    incidentStatus: String,
    questionnaire: Questionnaire,
    dsl: IncidentDsl.() -> Unit,
  ): Incident =
    incidentBuilderFactory!!.builder()
      .let { builder ->
        builder.build(
          title = title,
          description = description,
          prisonId = prisonId,
          reportingStaff = reportingStaff,
          reportedDateTime = reportedDateTime,
          incidentDateTime = incidentDateTime,
          incidentStatus = incidentStatus,
          questionnaire = questionnaire,
        )
          .also {
            builder.apply(dsl)
          }
      }

  @CSIPReportDslMarker
  override fun csipReport(
    offender: Offender,
    offenderBooking: OffenderBooking,
    type: String,
    location: String,
    areaOfWork: String,
    dsl: CSIPReportDsl.() -> Unit,
  ): CSIPReport =
    csipReportBuilderFactory!!.builder()
      .let { builder ->
        builder.build(
          offender = offender,
          offenderBooking = offenderBooking,
          type = type,
          location = location,
          areaOfWork = areaOfWork,
        )
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

  @AgencyInternalLocationDslMarker
  override fun agencyInternalLocation(
    locationCode: String,
    locationType: String,
    prisonId: String,
    parentAgencyInternalLocationId: Long?,
    capacity: Int?,
    operationalCapacity: Int?,
    cnaCapacity: Int?,
    userDescription: String?,
    listSequence: Int?,
    comment: String?,
    active: Boolean,
    deactivationDate: LocalDate?,
    reactivationDate: LocalDate?,
    dsl: AgencyInternalLocationDsl.() -> Unit,
  ): AgencyInternalLocation =
    agencyInternalLocationBuilderFactory!!.builder()
      .let { builder ->
        builder.build(
          locationCode = locationCode,
          locationType = locationType,
          prisonId = prisonId,
          parentAgencyInternalLocationId = parentAgencyInternalLocationId,
          capacity = capacity,
          operationalCapacity = operationalCapacity,
          cnaCapacity = cnaCapacity,
          userDescription = userDescription,
          listSequence = listSequence,
          comment = comment,
          active = active,
          deactivationDate = deactivationDate,
          reactivationDate = reactivationDate,
        )
          .also {
            builder.apply(dsl)
          }
      }

  override fun mergeTransaction(
    requestDate: LocalDateTime,
    requestStatusCode: String,
    transactionSource: String,
    offenderBookId1: Long,
    rootOffenderId1: Long,
    offenderId1: Long,
    nomsId1: String,
    lastName1: String,
    firstName1: String,
    offenderBookId2: Long,
    rootOffenderId2: Long,
    offenderId2: Long,
    nomsId2: String,
    lastName2: String,
    firstName2: String,
    dsl: MergeTransactionDsl.() -> Unit,
  ): MergeTransaction = mergeTransactionBuilderFactory!!.builder()
    .let { builder ->
      builder.build(
        requestDate = requestDate,
        requestStatusCode = requestStatusCode,
        transactionSource = transactionSource,
        offenderBookId1 = offenderBookId1,
        rootOffenderId1 = rootOffenderId1,
        offenderId1 = offenderId1,
        nomsId1 = nomsId1,
        lastName1 = lastName1,
        firstName1 = firstName1,
        offenderBookId2 = offenderBookId2,
        rootOffenderId2 = rootOffenderId2,
        offenderId2 = offenderId2,
        nomsId2 = nomsId2,
        lastName2 = lastName2,
        firstName2 = firstName2,
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

  @QuestionnaireDslMarker
  fun questionnaire(
    code: String,
    description: String = "This is a questionnaire",
    active: Boolean = true,
    listSequence: Int = 1,
    dsl: QuestionnaireDsl.() -> Unit = {},
  ): Questionnaire

  @IncidentDslMarker
  fun incident(
    title: String = "An incident occurred",
    description: String = "Fighting and shouting occurred in the prisoner's cell and a chair was thrown.",
    prisonId: String = "BXI",
    reportingStaff: Staff,
    reportedDateTime: LocalDateTime = LocalDateTime.parse("2024-01-02T09:30"),
    incidentDateTime: LocalDateTime = LocalDateTime.parse("2023-12-30T13:45"),
    incidentStatus: String = "AWAN",
    questionnaire: Questionnaire,
    dsl: IncidentDsl.() -> Unit = {},
  ): Incident

  @CSIPReportDslMarker
  fun csipReport(
    offender: Offender,
    offenderBooking: OffenderBooking,
    type: String = "INT",
    location: String = "LIB",
    areaOfWork: String = "EDU",
    dsl: CSIPReportDsl.() -> Unit = {},
  ): CSIPReport

  @ExternalServiceDslMarker
  fun externalService(
    serviceName: String,
    description: String = serviceName,
    dsl: ExternalServiceDsl.() -> Unit = {},
  ): ExternalService

  @AgencyInternalLocationDslMarker
  fun agencyInternalLocation(
    locationCode: String,
    locationType: String,
    prisonId: String,
    parentAgencyInternalLocationId: Long? = null,
    capacity: Int? = 1,
    operationalCapacity: Int? = null,
    cnaCapacity: Int? = null,
    userDescription: String? = null,
    listSequence: Int? = null,
    comment: String? = "comment",
    active: Boolean = true,
    deactivationDate: LocalDate? = null,
    reactivationDate: LocalDate? = null,
    dsl: AgencyInternalLocationDsl.() -> Unit = {},
  ): AgencyInternalLocation

  @MergeTransactionDslMarker
  fun mergeTransaction(
    requestDate: LocalDateTime = LocalDateTime.now(),
    requestStatusCode: String = "COMPLETED",
    transactionSource: String = "MANUAL",
    offenderBookId1: Long,
    rootOffenderId1: Long,
    offenderId1: Long = rootOffenderId1,
    nomsId1: String,
    lastName1: String = "DOYLEY",
    firstName1: String = "DEREK",
    offenderBookId2: Long,
    rootOffenderId2: Long,
    offenderId2: Long = rootOffenderId2,
    nomsId2: String,
    lastName2: String = "DOYLEY",
    firstName2: String = "DEREK",
    dsl: MergeTransactionDsl.() -> Unit = {},
  ): MergeTransaction
}

@DslMarker
annotation class NomisDataDslMarker
