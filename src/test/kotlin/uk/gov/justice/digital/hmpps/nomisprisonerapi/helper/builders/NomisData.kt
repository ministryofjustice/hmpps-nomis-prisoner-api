package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadCurrentAccountsBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ExternalService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPTemplate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LinkCaseTxn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MergeTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PostingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashScreen
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.math.BigDecimal
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
  private val splashScreenBuilderFactory: SplashScreenBuilderFactory? = null,
  private val agencyInternalLocationBuilderFactory: AgencyInternalLocationBuilderFactory? = null,
  private val questionnaireBuilderFactory: QuestionnaireBuilderFactory? = null,
  private val incidentBuilderFactory: IncidentBuilderFactory? = null,
  private val mergeTransactionBuilderFactory: MergeTransactionBuilderFactory? = null,
  private val templateBuilderFactory: IWPTemplateBuilderFactory? = null,
  private val personBuilderFactory: PersonBuilderFactory? = null,
  private val corporateBuilderFactory: CorporateBuilderFactory? = null,
  private val linkCaseTxnBuilderFactory: LinkCaseTxnBuilderFactory? = null,
  private val agencyAddressBuilderFactory: AgencyAddressBuilderFactory? = null,
  private val generalLedgerTransactionBuilderFactory: GeneralLedgerTransactionBuilderFactory? = null,
  private val caseloadCurrentAccountsBaseBuilderFactory: CaseloadCurrentAccountsBaseBuilderFactory? = null,
) {
  fun <T> runInTransaction(block: () -> T) = block()

  fun build(dsl: NomisData.() -> Unit) = NomisData(
    programServiceBuilderFactory,
    offenderBuilderFactory,
    staffBuilderFactory,
    adjudicationIncidentBuilderFactory,
    nonAssociationBuilderFactory,
    externalServiceBuilderFactory,
    splashScreenBuilderFactory,
    agencyInternalLocationBuilderFactory,
    questionnaireBuilderFactory,
    incidentBuilderFactory,
    mergeTransactionBuilderFactory,
    templateBuilderFactory,
    personBuilderFactory,
    corporateBuilderFactory,
    linkCaseTxnBuilderFactory,
    agencyAddressBuilderFactory,
    generalLedgerTransactionBuilderFactory,
    caseloadCurrentAccountsBaseBuilderFactory,
  ).apply(dsl)
}

class NomisData(
  private val programServiceBuilderFactory: ProgramServiceBuilderFactory? = null,
  private val offenderBuilderFactory: OffenderBuilderFactory? = null,
  private val staffBuilderFactory: StaffBuilderFactory? = null,
  private val adjudicationIncidentBuilderFactory: AdjudicationIncidentBuilderFactory? = null,
  private val nonAssociationBuilderFactory: NonAssociationBuilderFactory? = null,
  private val externalServiceBuilderFactory: ExternalServiceBuilderFactory? = null,
  private val splashScreenBuilderFactory: SplashScreenBuilderFactory? = null,
  private val agencyInternalLocationBuilderFactory: AgencyInternalLocationBuilderFactory? = null,
  private val questionnaireBuilderFactory: QuestionnaireBuilderFactory? = null,
  private val incidentBuilderFactory: IncidentBuilderFactory? = null,
  private val mergeTransactionBuilderFactory: MergeTransactionBuilderFactory? = null,
  private val templateBuilderFactory: IWPTemplateBuilderFactory? = null,
  private val personBuilderFactory: PersonBuilderFactory? = null,
  private val corporateBuilderFactory: CorporateBuilderFactory? = null,
  private val linkCaseTxnBuilderFactory: LinkCaseTxnBuilderFactory? = null,
  private val agencyAddressBuilderFactory: AgencyAddressBuilderFactory? = null,
  private val generalLedgerTransactionBuilderFactory: GeneralLedgerTransactionBuilderFactory? = null,
  private val caseloadCurrentAccountsBaseBuilderFactory: CaseloadCurrentAccountsBaseBuilderFactory? = null,
) : NomisDataDsl {
  override fun staff(firstName: String, lastName: String, dsl: StaffDsl.() -> Unit): Staff = staffBuilderFactory!!.builder()
    .let { builder ->
      builder.build(lastName, firstName)
        .also {
          builder.apply(dsl)
        }
    }

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

  override fun offender(
    nomsId: String,
    titleCode: String?,
    lastName: String,
    firstName: String,
    middleName: String?,
    middleName2: String?,
    birthDate: LocalDate?,
    birthPlace: String?,
    birthCountryCode: String?,
    ethnicityCode: String?,
    genderCode: String,
    nameTypeCode: String?,
    createDate: LocalDate,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: OffenderDsl.() -> Unit,
  ): Offender = offenderBuilderFactory!!.builder()
    .let { builder ->
      builder.build(nomsId, titleCode, lastName, firstName, middleName, middleName2, birthDate, birthPlace, birthCountryCode, ethnicityCode, genderCode, nameTypeCode, createDate, whenCreated, whoCreated)
        .also {
          builder.apply(dsl)
        }
    }

  override fun programService(
    programCode: String,
    programId: Long,
    description: String,
    active: Boolean,
    dsl: ProgramServiceDsl.() -> Unit,
  ): ProgramService = programServiceBuilderFactory!!.builder()
    .let { builder ->
      builder.build(programCode, description, active)
        .also {
          builder.apply(dsl)
        }
    }

  override fun questionnaire(
    code: String,
    description: String,
    active: Boolean,
    listSequence: Int,
    dsl: QuestionnaireDsl.() -> Unit,
  ): Questionnaire = questionnaireBuilderFactory!!.builder()
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

  override fun incident(
    id: Long,
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
  ): Incident = incidentBuilderFactory!!.builder()
    .let { builder ->
      builder.build(
        id = id,
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

  override fun nonAssociation(
    offender1: Offender,
    offender2: Offender,
    nonAssociationReason: String,
    recipNonAssociationReason: String,
    dsl: NonAssociationDsl.() -> Unit,
  ): OffenderNonAssociation = nonAssociationBuilderFactory!!.builder()
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

  override fun externalService(
    serviceName: String,
    description: String,
    dsl: ExternalServiceDsl.() -> Unit,
  ): ExternalService = externalServiceBuilderFactory!!.builder()
    .let { builder ->
      builder.build(
        serviceName,
        description,
      )
        .also {
          builder.apply(dsl)
        }
    }

  override fun splashScreen(
    moduleName: String,
    warningText: String?,
    accessBlockedCode: String,
    blockedText: String?,
    dsl: SplashScreenDsl.() -> Unit,
  ): SplashScreen = splashScreenBuilderFactory!!.builder()
    .let { builder ->
      builder.build(
        moduleName,
        warningText,
        accessBlockedCode,
        blockedText,
      )
        .also {
          builder.apply(dsl)
        }
    }

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
  ): AgencyInternalLocation = agencyInternalLocationBuilderFactory!!.builder()
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

  override fun template(
    name: String,
    description: String?,
    dsl: IWPTemplateDsl.() -> Unit,
  ): IWPTemplate = templateBuilderFactory!!.builder()
    .let { builder ->
      builder.build(name, description)
        .also {
          builder.apply(dsl)
        }
    }

  override fun corporate(
    corporateName: String,
    caseloadId: String?,
    commentText: String?,
    suspended: Boolean,
    feiNumber: String?,
    active: Boolean,
    expiryDate: LocalDate?,
    taxNo: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: CorporateDsl.() -> Unit,
  ): Corporate = corporateBuilderFactory!!.builder()
    .let { builder ->
      builder.build(
        corporateName = corporateName,
        caseloadId = caseloadId,
        commentText = commentText,
        suspended = suspended,
        feiNumber = feiNumber,
        active = active,
        expiryDate = expiryDate,
        taxNo = taxNo,
        whenCreated = whenCreated,
        whoCreated = whoCreated,
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
    whenCreated: LocalDateTime?,
    whoCreated: String?,
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
        whenCreated = whenCreated,
        whoCreated = whoCreated,
      )
        .also {
          builder.apply(dsl)
        }
    }

  override fun linkedCaseTransaction(
    sourceCase: CourtCase,
    targetCase: CourtCase,
    courtEvent: CourtEvent,
    offenderCharge: OffenderCharge,
    whenCreated: LocalDateTime?,
    dsl: LinkCaseTxnDsl.() -> Unit,
  ): LinkCaseTxn = linkCaseTxnBuilderFactory!!.builder()
    .let { builder ->
      builder.build(
        sourceCase = sourceCase,
        targetCase = targetCase,
        courtEvent = courtEvent,
        offenderCharge = offenderCharge,
        whenCreated = whenCreated,
      )
        .also {
          builder.apply(dsl)
        }
    }

  // TODO - I chickened out of mapping AGENCY_LOCATIONS because AGY_LOC_ID is everywhere. Left it for the person who migrates agencies.
  override fun agencyAddress(
    agencyLocationId: String,
    type: String?,
    premise: String?,
    street: String?,
    locality: String?,
    flat: String?,
    postcode: String?,
    city: String?,
    county: String?,
    country: String?,
    validatedPAF: Boolean,
    noFixedAddress: Boolean?,
    primaryAddress: Boolean,
    mailAddress: Boolean,
    comment: String?,
    startDate: String?,
    endDate: String?,
    isServices: Boolean,
    businessHours: String?,
    contactPersonName: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: AgencyAddressDsl.() -> Unit,
  ): AgencyAddress = agencyAddressBuilderFactory!!.builder().let { builder ->
    builder.build(
      agencyLocationId = agencyLocationId,
      type = type,
      premise = premise,
      street = street,
      locality = locality,
      flat = flat,
      postcode = postcode,
      city = city,
      county = county,
      country = country,
      validatedPAF = validatedPAF,
      noFixedAddress = noFixedAddress,
      primaryAddress = primaryAddress,
      mailAddress = mailAddress,
      comment = comment,
      startDate = startDate?.let { LocalDate.parse(startDate) },
      endDate = endDate?.let { LocalDate.parse(endDate) },
      isServices = isServices,
      businessHours = businessHours,
      contactPersonName = contactPersonName,
      whenCreated = whenCreated,
      whoCreated = whoCreated,
    )
  }

  override fun generalLedgerTransaction(
    transactionId: Long,
    transactionEntrySequence: Int,
    generalLedgerEntrySequence: Int,
    transactionType: String,
    accountCode: Int,
    postUsage: PostingType,
    entryDateTime: LocalDateTime,
    entryAmount: BigDecimal,
    dsl: GeneralLedgerTransactionDsl.() -> Unit,
  ): GeneralLedgerTransaction = generalLedgerTransactionBuilderFactory!!.builder().build(
    transactionId = transactionId,
    transactionEntrySequence = transactionEntrySequence,
    generalLedgerEntrySequence = generalLedgerEntrySequence,
    offender = null,
    caseloadId = "BXI",
    transactionType = transactionType,
    accountCode = accountCode,
    postUsage = postUsage,
    entryDateTime = entryDateTime,
    transactionReferenceNumber = null,
    entryAmount = entryAmount,
  )

  override fun caseloadCurrentAccountBase(
    caseloadId: String,
    accountCode: Int,
    accountPeriod: Int,
    currentBalance: BigDecimal,
    dsl: CaseloadCurrentAccountsBaseDsl.() -> Unit,
  ): CaseloadCurrentAccountsBase = caseloadCurrentAccountsBaseBuilderFactory!!.builder().let { builder ->
    builder.build(
      caseloadId = caseloadId,
      accountCode = accountCode,
      accountPeriod = accountPeriod,
      currentBalance = currentBalance,
    ).also { builder.apply(dsl) }
  }
}

@NomisDataDslMarker
interface NomisDataDsl {
  @OffenderDslMarker
  fun offender(
    nomsId: String = "A5194DY",
    titleCode: String? = null,
    lastName: String = "NTHANDA",
    firstName: String = "LEKAN",
    middleName: String? = null,
    middleName2: String? = null,
    birthDate: LocalDate? = LocalDate.of(1965, 7, 19),
    birthPlace: String? = null,
    birthCountryCode: String? = null,
    ethnicityCode: String? = null,
    genderCode: String = "M",
    nameTypeCode: String? = null,
    createDate: LocalDate = LocalDate.parse("2020-03-20"),
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
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
    id: Long,
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

  @SplashScreenDslMarker
  fun splashScreen(
    moduleName: String,
    warningText: String? = null,
    accessBlockedCode: String,
    blockedText: String? = null,
    dsl: SplashScreenDsl.() -> Unit = {},
  ): SplashScreen

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
    offenderBookId1: Long = 1,
    rootOffenderId1: Long = 1,
    offenderId1: Long = rootOffenderId1,
    nomsId1: String,
    lastName1: String = "DOYLEY",
    firstName1: String = "DEREK",
    offenderBookId2: Long = 2,
    rootOffenderId2: Long = 2,
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
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: PersonDsl.() -> Unit = {},
  ): Person

  @CorporateDslMarker
  fun corporate(
    corporateName: String,
    caseloadId: String? = null,
    commentText: String? = null,
    suspended: Boolean = false,
    feiNumber: String? = null,
    active: Boolean = true,
    expiryDate: LocalDate? = null,
    taxNo: String? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: CorporateDsl.() -> Unit = {},
  ): Corporate

  @LinkCaseTxnDslMarker
  fun linkedCaseTransaction(
    sourceCase: CourtCase,
    targetCase: CourtCase,
    courtEvent: CourtEvent,
    offenderCharge: OffenderCharge,
    whenCreated: LocalDateTime? = null,
    dsl: LinkCaseTxnDsl.() -> Unit = {},
  ): LinkCaseTxn

  @AgencyAddressDslMarker
  fun agencyAddress(
    agencyLocationId: String = "LEI",
    type: String? = null,
    premise: String? = "2",
    street: String? = "Gloucester Terrace",
    locality: String? = "Stanningley",
    flat: String? = null,
    postcode: String? = null,
    city: String? = null,
    county: String? = null,
    country: String? = null,
    validatedPAF: Boolean = false,
    noFixedAddress: Boolean? = null,
    primaryAddress: Boolean = false,
    mailAddress: Boolean = false,
    comment: String? = null,
    startDate: String? = null,
    endDate: String? = null,
    isServices: Boolean = false,
    businessHours: String? = null,
    contactPersonName: String? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: AgencyAddressDsl.() -> Unit = {},
  ): AgencyAddress

  @GeneralLedgerTransactionDslMarker
  fun generalLedgerTransaction(
    transactionId: Long,
    transactionEntrySequence: Int,
    generalLedgerEntrySequence: Int,
    transactionType: String = "SPEN",
    accountCode: Int = 2000,
    postUsage: PostingType = PostingType.CR,
    entryDateTime: LocalDateTime = LocalDateTime.parse("2025-08-08T12:13:14"),
    entryAmount: BigDecimal = BigDecimal.TWO,
    dsl: GeneralLedgerTransactionDsl.() -> Unit = {},
  ): GeneralLedgerTransaction

  @CaseloadCurrentAccountsBaseDslMarker
  fun caseloadCurrentAccountBase(
    caseloadId: String = "MDI",
    accountCode: Int = 2101,
    accountPeriod: Int = 202608,
    currentBalance: BigDecimal,
    dsl: CaseloadCurrentAccountsBaseDsl.() -> Unit = {},
  ): CaseloadCurrentAccountsBase
}

@DslMarker
annotation class NomisDataDslMarker
