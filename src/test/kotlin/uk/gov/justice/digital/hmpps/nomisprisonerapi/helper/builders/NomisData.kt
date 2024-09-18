package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ExternalService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPTemplate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MergeTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
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
  private val questionnaireBuilderFactory: QuestionnaireBuilderFactory? = null,
  private val incidentBuilderFactory: IncidentBuilderFactory? = null,
  private val mergeTransactionBuilderFactory: MergeTransactionBuilderFactory? = null,
  private val templateBuilderFactory: IWPTemplateBuilderFactory? = null,
  private val personBuilderFactory: PersonBuilderFactory? = null,
) {
  fun build(dsl: NomisData.() -> Unit) = NomisData(
    programServiceBuilderFactory,
    offenderBuilderFactory,
    staffBuilderFactory,
    adjudicationIncidentBuilderFactory,
    nonAssociationBuilderFactory,
    externalServiceBuilderFactory,
    agencyInternalLocationBuilderFactory,
    questionnaireBuilderFactory,
    incidentBuilderFactory,
    mergeTransactionBuilderFactory,
    templateBuilderFactory,
    personBuilderFactory,
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
  private val questionnaireBuilderFactory: QuestionnaireBuilderFactory? = null,
  private val incidentBuilderFactory: IncidentBuilderFactory? = null,
  private val mergeTransactionBuilderFactory: MergeTransactionBuilderFactory? = null,
  private val templateBuilderFactory: IWPTemplateBuilderFactory? = null,
  private val personBuilderFactory: PersonBuilderFactory? = null,

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
    locationId: String,
    reportingStaff: Staff,
    reportedDateTime: LocalDateTime,
    incidentDateTime: LocalDateTime,
    incidentStatus: String,
    followUpDate: LocalDate,
    questionnaire: Questionnaire,
    dsl: IncidentDsl.() -> Unit,
  ): Incident =
    incidentBuilderFactory!!.builder()
      .let { builder ->
        builder.build(
          title = title,
          description = description,
          agencyId = locationId,
          reportingStaff = reportingStaff,
          reportedDateTime = reportedDateTime,
          incidentDateTime = incidentDateTime,
          incidentStatus = incidentStatus,
          followUpDate = followUpDate,
          questionnaire = questionnaire,
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

  @IWPTemplateDslMarker
  override fun template(
    name: String,
    description: String?,
    dsl: IWPTemplateDsl.() -> Unit,
  ): IWPTemplate =
    templateBuilderFactory!!.builder()
      .let { builder ->
        builder.build(name, description)
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

  override fun person(
    firstName: String,
    lastName: String,
    middleName: String?,
    dateOfBirth: String?,
    gender: String?,
    title: String?,
    language: String?,
    interpreterRequired: Boolean,
    domesticStatus: String?,
    deceasedDate: String?,
    isStaff: Boolean?,
    isRemitter: Boolean?,
    keepBiometrics: Boolean,
    dsl: PersonDsl.() -> Unit,
  ): Person = personBuilderFactory!!.builder()
    .let { builder ->
      builder.build(
        lastName = lastName,
        firstName = firstName,
        middleName = middleName,
        dateOfBirth = dateOfBirth?.let { LocalDate.parse(it) },
        gender = gender,
        title = title,
        language = language,
        interpreterRequired = interpreterRequired,
        domesticStatus = domesticStatus,
        deceasedDate = deceasedDate?.let { LocalDate.parse(it) },
        isStaff = isStaff,
        isRemitter = isRemitter,
        keepBiometrics = keepBiometrics,
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
    locationId: String = "BXI",
    reportingStaff: Staff,
    reportedDateTime: LocalDateTime = LocalDateTime.parse("2024-01-02T09:30"),
    incidentDateTime: LocalDateTime = LocalDateTime.parse("2023-12-30T13:45"),
    incidentStatus: String = "AWAN",
    followUpDate: LocalDate = LocalDate.parse("2025-05-04"),
    questionnaire: Questionnaire,
    dsl: IncidentDsl.() -> Unit = {},
  ): Incident

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

  @IWPTemplateDslMarker
  fun template(
    name: String = "template1",
    description: String?,
    dsl: IWPTemplateDsl.() -> Unit = {},
  ): IWPTemplate

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

  @PersonDslMarker
  fun person(
    firstName: String = "AAYAN",
    lastName: String = "AHMAD",
    middleName: String? = null,
    dateOfBirth: String? = null,
    gender: String? = null,
    title: String? = null,
    language: String? = null,
    interpreterRequired: Boolean = false,
    domesticStatus: String? = null,
    deceasedDate: String? = null,
    isStaff: Boolean? = null,
    isRemitter: Boolean? = null,
    keepBiometrics: Boolean = false,
    dsl: PersonDsl.() -> Unit = {},
  ): Person
}

@DslMarker
annotation class NomisDataDslMarker
