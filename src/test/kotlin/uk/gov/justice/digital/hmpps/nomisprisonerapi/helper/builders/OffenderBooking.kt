package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.WITNESS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPDocument
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPTemplate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LinkCaseTxn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NoteSourceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlert
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBelief
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBookingImage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseNote
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderFixedTermRecall
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifyingMark
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderKeyDateAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributes
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderRestrictions
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@DslMarker
annotation class BookingDslMarker

@NomisDataDslMarker
interface BookingDsl {
  @OffenderRestrictionsDslMarker
  fun restriction(
    restrictionType: String = "BAN",
    enteredStaff: Staff,
    authorisedStaff: Staff,
    comment: String? = null,
    effectiveDate: LocalDate = LocalDate.now(),
    expiryDate: LocalDate? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: OffenderRestrictionsDsl.() -> Unit = {},
  ): OffenderRestrictions

  @AdjudicationPartyDslMarker
  fun adjudicationParty(
    incident: AdjudicationIncident,
    comment: String = "They witnessed everything",
    role: PartyRole = WITNESS,
    partyAddedDate: LocalDate = LocalDate.of(2023, 5, 10),
    staff: Staff? = null,
    adjudicationNumber: Long? = null,
    actionDecision: String = IncidentDecisionAction.NO_FURTHER_ACTION_CODE,
    dsl: AdjudicationPartyDsl.() -> Unit = {},
  ): AdjudicationIncidentParty

  @IncentiveDslMarker
  fun incentive(
    iepLevelCode: String = "ENT",
    userId: String? = null,
    sequence: Long = 1,
    commentText: String = "comment",
    auditModuleName: String? = null,
    iepDateTime: LocalDateTime = LocalDateTime.now(),
  ): Incentive

  @CourseAllocationDslMarker
  fun courseAllocation(
    courseActivity: CourseActivity,
    startDate: String? = "2022-10-31",
    programStatusCode: String = "ALLOC",
    endDate: String? = null,
    endReasonCode: String? = null,
    endComment: String? = null,
    suspended: Boolean = false,
    prisonId: String? = null,
    dsl: CourseAllocationDsl.() -> Unit = { payBand() },
  ): OffenderProgramProfile

  @CSIPReportDslMarker
  fun csipReport(
    type: String = "INT",
    location: String = "LIB",
    areaOfWork: String = "EDU",
    reportedBy: String = "Jane Reporter",
    incidentDate: LocalDate = LocalDate.now(),
    incidentTime: LocalTime? = null,
    staffAssaulted: Boolean = false,
    staffAssaultedName: String? = null,
    releaseDate: LocalDate? = null,
    involvement: String? = null,
    concern: String? = null,
    knownReasons: String? = null,
    otherInformation: String? = null,
    referralComplete: Boolean = false,
    referralCompletedBy: String? = null,
    referralCompletedDate: LocalDate? = null,
    caseManager: String? = null,
    planReason: String? = null,
    firstCaseReviewDate: LocalDate? = null,
    logNumber: String? = null,
    dsl: CSIPReportDsl.() -> Unit = {},
  ): CSIPReport

  @IWPDocumentDslMarker
  fun document(
    fileName: String = "doc1.txt",
    template: IWPTemplate,
    status: String = "PUBLIC",
    body: String? = null,
    dsl: IWPDocumentDsl.() -> Unit = {},
  ): IWPDocument

  @OffenderSentenceDslMarker
  fun sentence(
    calculationType: String = "ADIMP_ORA",
    category: String = "2003",
    startDate: LocalDate = LocalDate.of(2023, 1, 1),
    status: String = "I",
    sentenceLevel: String = "AGG",
    consecSequence: Int? = 2,
    courtOrder: CourtOrder? = null,
    endDate: LocalDate = LocalDate.of(2023, 1, 5),
    commentText: String? = "a sentence comment",
    absenceCount: Int? = 2,
    etdCalculatedDate: LocalDate? = LocalDate.parse("2023-01-02"),
    mtdCalculatedDate: LocalDate? = LocalDate.parse("2023-01-03"),
    ltdCalculatedDate: LocalDate? = LocalDate.parse("2023-01-04"),
    ardCalculatedDate: LocalDate? = LocalDate.parse("2023-01-05"),
    crdCalculatedDate: LocalDate? = LocalDate.parse("2023-01-06"),
    pedCalculatedDate: LocalDate? = LocalDate.parse("2023-01-07"),
    npdCalculatedDate: LocalDate? = LocalDate.parse("2023-01-08"),
    ledCalculatedDate: LocalDate? = LocalDate.parse("2023-01-09"),
    sedCalculatedDate: LocalDate? = LocalDate.parse("2023-01-10"),
    prrdCalculatedDate: LocalDate? = LocalDate.parse("2023-01-11"),
    tariffCalculatedDate: LocalDate? = LocalDate.parse("2023-01-12"),
    dprrdCalculatedDate: LocalDate? = LocalDate.parse("2023-01-13"),
    tusedCalculatedDate: LocalDate? = LocalDate.parse("2023-01-14"),
    aggAdjustDays: Int? = 6,
    aggSentenceSequence: Int? = 3,
    extendedDays: Int? = 4,
    counts: Int? = 5,
    statusUpdateReason: String? = "update rsn",
    statusUpdateComment: String? = "update comment",
    statusUpdateDate: LocalDate? = LocalDate.parse("2023-01-05"),
    statusUpdateStaff: Staff? = null,
    fineAmount: BigDecimal? = BigDecimal.valueOf(12.5),
    dischargeDate: LocalDate? = LocalDate.parse("2023-01-05"),
    nomSentDetailRef: Long? = 11,
    nomConsToSentDetailRef: Long? = 12,
    nomConsFromSentDetailRef: Long? = 13,
    nomConsWithSentDetailRef: Long? = 14,
    lineSequence: Int? = 1,
    hdcExclusionFlag: Boolean? = true,
    hdcExclusionReason: String? = "hdc reason",
    cjaAct: String? = "A",
    sled2Calc: LocalDate? = LocalDate.parse("2023-01-20"),
    startDate2Calc: LocalDate? = LocalDate.parse("2023-01-21"),
    dsl: OffenderSentenceDsl.() -> Unit = { },
  ): OffenderSentence

  @OffenderKeyDateAdjustmentDslMarker
  fun adjustment(
    adjustmentTypeCode: String = "ADA",
    adjustmentDate: LocalDate = LocalDate.now(),
    // used in migration date filtering
    createdDate: LocalDateTime = LocalDateTime.now(),
    adjustmentNumberOfDays: Long = 10,
    active: Boolean = true,
    dsl: OffenderKeyDateAdjustmentDsl.() -> Unit = { },
  ): OffenderKeyDateAdjustment

  @CourtCaseDslMarker
  fun courtCase(
    whenCreated: LocalDateTime = LocalDateTime.now(),
    caseStatus: String = "A",
    legalCaseType: String = "A",
    beginDate: LocalDate = LocalDate.now(),
    caseSequence: Int = 1,
    caseInfoNumber: String? = "AB1",
    agencyId: String = "COURT1",
    combinedCase: CourtCase? = null,
    reportingStaff: Staff,
    statusUpdateStaff: Staff? = null,
    statusUpdateDate: LocalDate? = null,
    statusUpdateReason: String? = "a reason",
    statusUpdateComment: String? = "a comment",
    lidsCaseNumber: Int = 1,
    lidsCaseId: Int? = 2,
    lidsCombinedCaseId: Int? = 3,
    dsl: CourtCaseDsl.() -> Unit = { },
  ): CourtCase

  fun linkCases(
    sourceCourtCase: CourtCase,
    targetCourtCase: CourtCase,
    targetCourtEvent: CourtEvent,
    whenCreated: LocalDateTime = LocalDateTime.now(),
  ): List<LinkCaseTxn>

  @OffenderFixedTermRecallDslMarker
  fun fixedTermRecall(
    returnToCustodyDate: LocalDate = LocalDate.now(),
    comments: String? = null,
    recallLength: Long = 28,
    staff: Staff,
    dsl: OffenderFixedTermRecallDsl.() -> Unit = { },
  ): OffenderFixedTermRecall

  @OffenderAlertDslMarker
  fun alert(
    sequence: Long? = null,
    alertCode: String = "XA",
    typeCode: String = "X",
    date: LocalDate = LocalDate.now(),
    expiryDate: LocalDate? = null,
    authorizePersonText: String? = null,
    status: AlertStatus = AlertStatus.ACTIVE,
    commentText: String? = null,
    verifiedFlag: Boolean = false,
    createUsername: String = "SA",
    modifyUsername: String? = null,
    dsl: OffenderAlertDsl.() -> Unit = { },
  ): OffenderAlert

  @OffenderCaseNoteDslMarker
  fun caseNote(
    caseNoteType: String,
    caseNoteSubType: String,
    date: LocalDateTime = LocalDateTime.now(),
    author: Staff,
    caseNoteText: String,
    amendmentFlag: Boolean = false,
    noteSourceCode: NoteSourceCode = NoteSourceCode.INST,
    timeCreation: LocalDateTime? = date,
    dsl: OffenderCaseNoteDsl.() -> Unit = { },
  ): OffenderCaseNote

  @OffenderPhysicalAttributesDslMarker
  fun physicalAttributes(
    heightCentimetres: Int? = 180,
    heightFeet: Int? = null,
    heightInches: Int? = null,
    weightKilograms: Int? = 80,
    weightPounds: Int? = null,
    sequence: Long? = null,
  ): OffenderPhysicalAttributes

  @OffenderProfileDslMarker
  fun profile(
    checkDate: LocalDateTime = LocalDateTime.now(),
    sequence: Int = 1,
    dsl: OffenderProfileDsl.() -> Unit = {},
  ): OffenderProfile

  @OffenderProfileDetailDslMarker
  fun profileDetail(
    listSequence: Int = 99,
    profileType: String = "BUILD",
    profileCode: String? = "SMALL",
    sequence: Int = 1,
  ): OffenderProfileDetail

  @OffenderExternalMovementDslMarker
  fun prisonTransfer(
    from: String = "BXI",
    to: String = "MDI",
    date: LocalDateTime = LocalDateTime.now(),
  ): Pair<OffenderExternalMovement, OffenderExternalMovement>

  @OffenderExternalMovementDslMarker
  fun release(
    date: LocalDateTime = LocalDateTime.now(),
  ): OffenderExternalMovement

  @OffenderExternalMovementDslMarker
  fun receive(
    date: LocalDateTime = LocalDateTime.now(),
  ): OffenderExternalMovement

  @OffenderExternalMovementDslMarker
  fun temporaryAbsence(
    date: LocalDateTime = LocalDateTime.now(),
    fromPrison: String = "BXI",
    toAgency: String? = null,
    movementReason: String = "C5",
    arrestAgency: String? = null,
    escort: String? = null,
    escortText: String? = null,
    comment: String? = null,
    toCity: String? = null,
    toAddress: Address? = null,
  ): OffenderTemporaryAbsence

  @OffenderExternalMovementDslMarker
  fun temporaryAbsenceReturn(
    date: LocalDateTime = LocalDateTime.now(),
    fromAgency: String? = null,
    toPrison: String = "BXI",
    movementReason: String = "C5",
    escort: String? = null,
    escortText: String? = null,
    comment: String? = null,
    fromCity: String? = null,
    fromAddress: Address? = null,
  ): OffenderTemporaryAbsenceReturn

  @VisitBalanceDslMarker
  fun visitBalance(
    remainingVisitOrders: Int? = 7,
    remainingPrivilegedVisitOrders: Int? = 4,
    dsl: VisitBalanceDsl.() -> Unit = {},
  ): OffenderVisitBalance

  @VisitDslMarker
  fun visit(
    visitTypeCode: String = "SCON",
    visitStatusCode: String = "SCH",
    startDateTimeString: String = "2022-01-01T12:05",
    endDateTimeString: String = "2022-01-01T13:05",
    agyLocId: String = "MDI",
    agencyInternalLocationDescription: String? = "MDI-1-1-001",
    createdDatetime: LocalDateTime? = null,
    dsl: VisitDsl.() -> Unit = {},
  ): Visit

  fun visitOrder(
    orderNumber: Long,
    visitOrderTypeCode: String = "VO",
    visitStatusCode: String = "SCH",
    issueDate: LocalDate = LocalDate.now(),
    dsl: VisitOrderDsl.() -> Unit = {},
  ): VisitOrder

  @VisitDslMarker
  fun officialVisit(
    visitTypeCode: String = "OFFI",
    visitStatusCode: String = "SCH",
    visitDate: LocalDate = LocalDate.parse("2022-01-01"),
    visitSlot: AgencyVisitSlot,
    comment: String? = null,
    visitorConcern: String? = null,
    overrideBanStaff: Staff? = null,
    prisonerSearchTypeCode: String? = null,
    visitOrder: VisitOrder? = null,
    dsl: VisitDsl.() -> Unit = {},
  ): Visit

  @OffenderContactPersonDslMarker
  fun contact(
    person: Person,
    relationshipType: String = "FRI",
    contactType: String = "S",
    active: Boolean = true,
    nextOfKin: Boolean = false,
    emergencyContact: Boolean = false,
    approvedVisitor: Boolean = true,
    comment: String? = null,
    expiryDate: String? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: OffenderContactPersonDsl.() -> Unit = {},
  ): OffenderContactPerson

  @OffenderBookingImageDslMarker
  fun image(
    captureDateTime: LocalDateTime = LocalDateTime.now(),
    fullSizeImage: ByteArray = byteArrayOf(1, 2, 3),
    thumbnailImage: ByteArray = byteArrayOf(4, 5, 6),
    active: Boolean = true,
    imageSourceCode: String = "FILE",
    dsl: OffenderBookingImageDsl.() -> Unit = {},
  ): OffenderBookingImage

  @OffenderIdentifyingMarkDslMarker
  fun identifyingMark(
    sequence: Long = 1,
    bodyPartCode: String = "HEAD",
    markTypeCode: String = "TAT",
    sideCode: String? = "L",
    partOrientationCode: String? = "FACE",
    commentText: String? = "head tattoo left front",
    dsl: OffenderIdentifyingMarkDsl.() -> Unit = {},
  ): OffenderIdentifyingMark

  @OffenderBeliefDslMarker
  fun belief(
    beliefCode: String,
    startDate: LocalDate = LocalDate.parse("2021-01-01"),
    endDate: LocalDate? = null,
    changeReason: Boolean? = null,
    comments: String? = null,
    verified: Boolean? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: OffenderBeliefDsl.() -> Unit = {},
  ): OffenderBelief

  @OffenderTransactionDslMarker
  fun transaction(
    transactionId: Long = 1,
    transactionEntrySequence: Int = 1,
    transactionType: String,
    entryDate: LocalDate = LocalDate.parse("2025-06-01"),
    dsl: OffenderTransactionDsl.() -> Unit = {},
  ): OffenderTransaction

  @OffenderTemporaryAbsenceApplicationDslMarker
  fun temporaryAbsenceApplication(
    eventSubType: String = "C5",
    applicationDate: LocalDateTime = LocalDateTime.now(),
    applicationTime: LocalDateTime = LocalDateTime.now(),
    fromDate: LocalDate = LocalDate.now(),
    releaseTime: LocalDateTime = LocalDateTime.now(),
    toDate: LocalDate = LocalDate.now().plusDays(1),
    returnTime: LocalDateTime = LocalDateTime.now().plusDays(1),
    applicationStatus: String = "APP-SCH",
    escort: String? = "L",
    transportType: String? = "VAN",
    comment: String? = "Some application comment",
    toAddress: Address? = null,
    prison: String = "LEI",
    toAgency: String? = "HAZLWD",
    contactPersonName: String? = null,
    applicationType: String = "SINGLE",
    temporaryAbsenceType: String? = "RR",
    temporaryAbsenceSubType: String? = "RDR",
    dsl: OffenderTemporaryAbsenceApplicationDsl.() -> Unit = {},
  ): OffenderMovementApplication
}

@Component
class BookingBuilderRepository(
  private val offenderBookingRepository: OffenderBookingRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
) {
  fun save(offenderBooking: OffenderBooking): OffenderBooking = offenderBookingRepository.save(offenderBooking)
  fun lookupAgencyLocation(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
  fun lookupAgencyInternalLocation(id: Long): AgencyInternalLocation = agencyInternalLocationRepository.findByIdOrNull(id)!!
}

@Component
class BookingBuilderFactory(
  private val repository: BookingBuilderRepository,
  private val courseAllocationBuilderFactory: CourseAllocationBuilderFactory,
  private val incentiveBuilderFactory: IncentiveBuilderFactory,
  private val adjudicationPartyBuilderFactory: AdjudicationPartyBuilderFactory,
  private val offenderSentenceBuilderFactory: OffenderSentenceBuilderFactory,
  private val courtCaseBuilderFactory: CourtCaseBuilderFactory,
  private val offenderFixedTermRecallBuilderFactory: OffenderFixedTermRecallBuilderFactory,
  private val offenderKeyDateAdjustmentBuilderFactory: OffenderKeyDateAdjustmentBuilderFactory,
  private val offenderExternalMovementBuilderFactory: OffenderExternalMovementBuilderFactory,
  private val offenderAlertBuilderFactory: OffenderAlertBuilderFactory,
  private val offenderCaseNoteBuilderFactory: OffenderCaseNoteBuilderFactory,
  private val csipReportBuilderFactory: CSIPReportBuilderFactory,
  private val documentBuilderFactory: IWPDocumentBuilderFactory,
  private val offenderPhysicalAttributesBuilderFactory: OffenderPhysicalAttributesBuilderFactory,
  private val offenderProfileBuilderFactory: OffenderProfileBuilderFactory,
  private val visitBalanceBuilderFactory: VisitBalanceBuilderFactory,
  private val offenderContactPersonBuilderFactory: OffenderContactPersonBuilderFactory,
  private val visitBuilderFactory: VisitBuilderFactory,
  private val visitOrderBuilderFactory: VisitOrderBuilderFactory,
  private val profileDetailBuilderFactory: OffenderProfileDetailBuilderFactory,
  private val offenderBookingImageBuilderFactory: OffenderBookingImageBuilderFactory,
  private val offenderIdentifyingMarkBuilderFactory: OffenderIdentifyingMarkBuilderFactory,
  private val offenderBeliefBuilderFactory: OffenderBeliefBuilderFactory,
  private val offenderTransactionBuilderFactory: OffenderTransactionBuilderFactory,
  private val offenderRestrictionsBuilderFactory: OffenderRestrictionsBuilderFactory,
  private val offenderTemporaryAbsenceApplicationBuilderFactory: OffenderTemporaryAbsenceApplicationBuilderFactory,
  private val linkCaseTxnBuilderFactory: LinkCaseTxnBuilderFactory,
) {
  fun builder() = BookingBuilder(
    repository,
    courseAllocationBuilderFactory,
    incentiveBuilderFactory,
    adjudicationPartyBuilderFactory,
    offenderSentenceBuilderFactory,
    courtCaseBuilderFactory,
    offenderFixedTermRecallBuilderFactory,
    offenderKeyDateAdjustmentBuilderFactory,
    offenderExternalMovementBuilderFactory,
    offenderAlertBuilderFactory,
    offenderCaseNoteBuilderFactory,
    csipReportBuilderFactory,
    documentBuilderFactory,
    offenderPhysicalAttributesBuilderFactory,
    offenderProfileBuilderFactory,
    visitBalanceBuilderFactory,
    offenderContactPersonBuilderFactory,
    visitBuilderFactory,
    visitOrderBuilderFactory,
    profileDetailBuilderFactory,
    offenderBookingImageBuilderFactory,
    offenderIdentifyingMarkBuilderFactory,
    offenderBeliefBuilderFactory,
    offenderTransactionBuilderFactory,
    offenderRestrictionsBuilderFactory,
    offenderTemporaryAbsenceApplicationBuilderFactory,
    linkCaseTxnBuilderFactory,
  )
}

class BookingBuilder(
  private val repository: BookingBuilderRepository,
  private val courseAllocationBuilderFactory: CourseAllocationBuilderFactory,
  private val incentiveBuilderFactory: IncentiveBuilderFactory,
  private val adjudicationPartyBuilderFactory: AdjudicationPartyBuilderFactory,
  private val offenderSentenceBuilderFactory: OffenderSentenceBuilderFactory,
  private val courtCaseBuilderFactory: CourtCaseBuilderFactory,
  private val offenderFixedTermRecallBuilderFactory: OffenderFixedTermRecallBuilderFactory,
  private val offenderKeyDateAdjustmentBuilderFactory: OffenderKeyDateAdjustmentBuilderFactory,
  private val offenderExternalMovementBuilderFactory: OffenderExternalMovementBuilderFactory,
  private val offenderAlertBuilderFactory: OffenderAlertBuilderFactory,
  private val offenderCaseNoteBuilderFactory: OffenderCaseNoteBuilderFactory,
  private val csipReportBuilderFactory: CSIPReportBuilderFactory,
  private val documentBuilderFactory: IWPDocumentBuilderFactory,
  private val offenderPhysicalAttributesBuilderFactory: OffenderPhysicalAttributesBuilderFactory,
  private val offenderProfileBuilderFactory: OffenderProfileBuilderFactory,
  private val visitBalanceBuilderFactory: VisitBalanceBuilderFactory,
  private val offenderContactPersonBuilderFactory: OffenderContactPersonBuilderFactory,
  private val visitBuilderFactory: VisitBuilderFactory,
  private val visitOrderBuilderFactory: VisitOrderBuilderFactory,
  private val profileDetailBuilderFactory: OffenderProfileDetailBuilderFactory,
  private val offenderBookingImageBuilderFactory: OffenderBookingImageBuilderFactory,
  private val offenderIdentifyingMarkBuilderFactory: OffenderIdentifyingMarkBuilderFactory,
  private val offenderBeliefBuilderFactory: OffenderBeliefBuilderFactory,
  private val offenderTransactionBuilderFactory: OffenderTransactionBuilderFactory,
  private val offenderRestrictionsBuilderFactory: OffenderRestrictionsBuilderFactory,
  private val offenderTemporaryAbsenceApplicationBuilderFactory: OffenderTemporaryAbsenceApplicationBuilderFactory,
  private val linkCaseTxnBuilderFactory: LinkCaseTxnBuilderFactory,
) : BookingDsl {

  private lateinit var offenderBooking: OffenderBooking

  fun build(
    offender: Offender,
    bookingSequence: Int,
    agencyLocationCode: String,
    bookingBeginDate: LocalDateTime = LocalDateTime.now(),
    bookingEndDate: LocalDate? = null,
    active: Boolean,
    inOutStatus: String,
    youthAdultCode: String,
    livingUnitId: Long,
  ): OffenderBooking {
    val agencyLocation = repository.lookupAgencyLocation(agencyLocationCode)
    return OffenderBooking(
      offender = offender,
      rootOffender = offender.rootOffender,
      bookingSequence = bookingSequence,
      createLocation = agencyLocation,
      location = agencyLocation,
      bookingBeginDate = bookingBeginDate,
      // note this overrides the booking end date set by the release() function
      bookingEndDate = bookingEndDate?.atStartOfDay(),
      active = active,
      inOutStatus = inOutStatus,
      youthAdultCode = youthAdultCode,
      assignedLivingUnit = repository.lookupAgencyInternalLocation(livingUnitId),
    )
      .let { repository.save(it) }
      .also {
        offenderExternalMovementBuilderFactory.builder()
          .build(
            toPrisonId = agencyLocationCode,
            date = bookingBeginDate,
            fromPrisonId = "COURT1",
            movementReason = "N",
            movementType = "ADM",
            offenderBooking = it,
          ).also { movement -> it.externalMovements += movement }
      }
      .also { offenderBooking = it }
  }

  override fun courseAllocation(
    courseActivity: CourseActivity,
    startDate: String?,
    programStatusCode: String,
    endDate: String?,
    endReasonCode: String?,
    endComment: String?,
    suspended: Boolean,
    prisonId: String?,
    dsl: CourseAllocationDsl.() -> Unit,
  ) = courseAllocationBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offenderBooking,
        startDate,
        programStatusCode,
        endDate,
        endReasonCode,
        endComment,
        suspended,
        prisonId,
        courseActivity,
      )
        .also { offenderBooking.offenderProgramProfiles += it }
        .also { courseActivity.offenderProgramProfiles += it }
        .also { builder.apply(dsl) }
    }

  override fun sentence(
    calculationType: String,
    category: String,
    startDate: LocalDate,
    status: String,
    sentenceLevel: String,
    consecSequence: Int?,
    courtOrder: CourtOrder?,
    endDate: LocalDate,
    commentText: String?,
    absenceCount: Int?,
    etdCalculatedDate: LocalDate?,
    mtdCalculatedDate: LocalDate?,
    ltdCalculatedDate: LocalDate?,
    ardCalculatedDate: LocalDate?,
    crdCalculatedDate: LocalDate?,
    pedCalculatedDate: LocalDate?,
    npdCalculatedDate: LocalDate?,
    ledCalculatedDate: LocalDate?,
    sedCalculatedDate: LocalDate?,
    prrdCalculatedDate: LocalDate?,
    tariffCalculatedDate: LocalDate?,
    dprrdCalculatedDate: LocalDate?,
    tusedCalculatedDate: LocalDate?,
    aggAdjustDays: Int?,
    aggSentenceSequence: Int?,
    extendedDays: Int?,
    counts: Int?,
    statusUpdateReason: String?,
    statusUpdateComment: String?,
    statusUpdateDate: LocalDate?,
    statusUpdateStaff: Staff?,
    fineAmount: BigDecimal?,
    dischargeDate: LocalDate?,
    nomSentDetailRef: Long?,
    nomConsToSentDetailRef: Long?,
    nomConsFromSentDetailRef: Long?,
    nomConsWithSentDetailRef: Long?,
    lineSequence: Int?,
    hdcExclusionFlag: Boolean?,
    hdcExclusionReason: String?,
    cjaAct: String?,
    sled2Calc: LocalDate?,
    startDate2Calc: LocalDate?,
    dsl: OffenderSentenceDsl.() -> Unit,
  ): OffenderSentence = offenderSentenceBuilderFactory.builder()
    .let { builder ->
      builder.build(
        courtCase = null,
        calculationType = calculationType,
        category = category,
        startDate = startDate,
        status = status,
        offenderBooking = offenderBooking,
        sequence = offenderBooking.sentences.size.toLong() + 1,
        sentenceLevel = sentenceLevel,
        consecLineSequence = consecSequence,
        courtOrder = courtOrder,
        endDate = endDate,
        commentText = commentText,
        absenceCount = absenceCount,
        etdCalculatedDate = etdCalculatedDate,
        mtdCalculatedDate = mtdCalculatedDate,
        ltdCalculatedDate = ltdCalculatedDate,
        ardCalculatedDate = ardCalculatedDate,
        crdCalculatedDate = crdCalculatedDate,
        pedCalculatedDate = pedCalculatedDate,
        npdCalculatedDate = npdCalculatedDate,
        ledCalculatedDate = ledCalculatedDate,
        sedCalculatedDate = sedCalculatedDate,
        prrdCalculatedDate = prrdCalculatedDate,
        tariffCalculatedDate = tariffCalculatedDate,
        dprrdCalculatedDate = dprrdCalculatedDate,
        tusedCalculatedDate = tusedCalculatedDate,
        aggAdjustDays = aggAdjustDays,
        aggSentenceSequence = aggSentenceSequence,
        extendedDays = extendedDays,
        counts = counts,
        statusUpdateReason = statusUpdateReason,
        statusUpdateComment = statusUpdateComment,
        statusUpdateDate = statusUpdateDate,
        statusUpdateStaff = statusUpdateStaff,
        fineAmount = fineAmount,
        dischargeDate = dischargeDate,
        nomSentDetailRef = nomSentDetailRef,
        nomConsToSentDetailRef = nomConsToSentDetailRef,
        nomConsFromSentDetailRef = nomConsFromSentDetailRef,
        nomConsWithSentDetailRef = nomConsWithSentDetailRef,
        lineSequence = lineSequence,
        hdcExclusionFlag = hdcExclusionFlag,
        hdcExclusionReason = hdcExclusionReason,
        cjaAct = cjaAct,
        sled2Calc = sled2Calc,
        startDate2Calc = startDate2Calc,
      )
        .also { offenderBooking.sentences += it }
        .also { builder.apply(dsl) }
    }

  override fun csipReport(
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
    dsl: CSIPReportDsl.() -> Unit,
  ): CSIPReport = csipReportBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offenderBooking = offenderBooking,
        type = type,
        location = location,
        areaOfWork = areaOfWork,
        reportedBy = reportedBy,
        incidentDate = incidentDate,
        incidentTime = incidentTime,
        releaseDate = releaseDate,
        involvement = involvement,
        concern = concern,
        staffAssaulted = staffAssaulted,
        staffAssaultedName = staffAssaultedName,
        knownReasons = knownReasons,
        otherInformation = otherInformation,
        referralComplete = referralComplete,
        referralCompletedBy = referralCompletedBy,
        referralCompletedDate = referralCompletedDate,
        caseManager = caseManager,
        planReason = planReason,
        firstCaseReviewDate = firstCaseReviewDate,
        logNumber = logNumber,
      )
        .also {
          builder.apply(dsl)
        }
    }

  @CourtCaseDslMarker
  override fun courtCase(
    whenCreated: LocalDateTime,
    caseStatus: String,
    legalCaseType: String,
    beginDate: LocalDate,
    caseSequence: Int,
    caseInfoNumber: String?,
    agencyId: String,
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
  ): CourtCase = courtCaseBuilderFactory.builder()
    .let { builder ->
      builder.build(
        whenCreated = whenCreated,
        offenderBooking = offenderBooking,
        combinedCase = combinedCase,
        caseStatus = caseStatus,
        legalCaseType = legalCaseType,
        caseSequence = caseSequence,
        beginDate = beginDate,
        caseInfoNumber = caseInfoNumber,
        agencyId = agencyId,
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

  override fun linkCases(
    sourceCourtCase: CourtCase,
    targetCourtCase: CourtCase,
    targetCourtEvent: CourtEvent,
    whenCreated: LocalDateTime,
  ): List<LinkCaseTxn> = linkCaseTxnBuilderFactory.builder().linkCases(
    sourceCase = sourceCourtCase,
    targetCase = targetCourtCase,
    courtEvent = targetCourtEvent,
    whenCreated = whenCreated,
  )

  override fun fixedTermRecall(
    returnToCustodyDate: LocalDate,
    comments: String?,
    recallLength: Long,
    staff: Staff,
    dsl: OffenderFixedTermRecallDsl.() -> Unit,
  ): OffenderFixedTermRecall = offenderFixedTermRecallBuilderFactory.builder()
    .let { builder ->
      builder.build(
        booking = offenderBooking,
        returnToCustodyDate = returnToCustodyDate,
        comments = comments,
        staff = staff,
        recallLength = recallLength,
      )
        .also {
          builder.apply(dsl)
        }
    }

  override fun alert(
    sequence: Long?,
    alertCode: String,
    typeCode: String,
    date: LocalDate,
    expiryDate: LocalDate?,
    authorizePersonText: String?,
    status: AlertStatus,
    commentText: String?,
    verifiedFlag: Boolean,
    createUsername: String,
    modifyUsername: String?,
    dsl: OffenderAlertDsl.() -> Unit,
  ): OffenderAlert = offenderAlertBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offenderBooking,
        sequence,
        alertCode,
        typeCode,
        date,
        expiryDate,
        authorizePersonText,
        status,
        commentText,
        verifiedFlag,
        createUsername,
        modifyUsername,
      ).also {
        builder.apply(dsl)
      }
    }

  override fun caseNote(
    caseNoteType: String,
    caseNoteSubType: String,
    date: LocalDateTime,
    author: Staff,
    caseNoteText: String,
    amendmentFlag: Boolean,
    noteSourceCode: NoteSourceCode,
    timeCreation: LocalDateTime?,
    dsl: OffenderCaseNoteDsl.() -> Unit,
  ): OffenderCaseNote = offenderCaseNoteBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offenderBooking,
        caseNoteType,
        caseNoteSubType,
        date,
        author,
        caseNoteText,
        amendmentFlag,
        noteSourceCode,
        timeCreation,
      ).also {
        builder.apply(dsl)
      }
    }

  override fun physicalAttributes(
    heightCentimetres: Int?,
    heightFeet: Int?,
    heightInches: Int?,
    weightKilograms: Int?,
    weightPounds: Int?,
    sequence: Long?,
  ): OffenderPhysicalAttributes = offenderPhysicalAttributesBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offenderBooking = offenderBooking,
        sequence = sequence,
        heightCentimetres = heightCentimetres,
        heightFeet = heightFeet,
        heightInches = heightInches,
        weightKilograms = weightKilograms,
        weightPounds = weightPounds,
      ).also {
        offenderBooking.physicalAttributes += it
      }
    }

  override fun profile(
    checkDate: LocalDateTime,
    sequence: Int,
    dsl: OffenderProfileDsl.() -> Unit,
  ): OffenderProfile = offenderProfileBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offenderBooking = offenderBooking,
        checkDate = checkDate,
        sequence = sequence,
      ).also {
        offenderBooking.profiles += it
        builder.apply(dsl)
      }
    }

  override fun profileDetail(
    listSequence: Int,
    profileType: String,
    profileCode: String?,
    sequence: Int,
  ): OffenderProfileDetail = profileDetailBuilderFactory.builder().build(
    listSequence = listSequence,
    profileTypeId = profileType,
    profileCodeId = profileCode,
    offenderBooking = offenderBooking,
    sequence = sequence,
  ).also {
    offenderBooking.profileDetails += it
  }

  override fun prisonTransfer(
    from: String,
    to: String,
    date: LocalDateTime,
  ): Pair<OffenderExternalMovement, OffenderExternalMovement> = offenderExternalMovementBuilderFactory.builder()
    .let { builder ->
      builder.buildTransfer(
        offenderBooking = offenderBooking,
        fromPrisonId = from,
        toPrisonId = to,
        date = date,
      )
        .also { offenderBooking.externalMovements.forEach { it.active = false } }
        .also { offenderBooking.externalMovements += it.first }
        .also { offenderBooking.externalMovements += it.second }
    }

  override fun release(
    date: LocalDateTime,
  ): OffenderExternalMovement = offenderExternalMovementBuilderFactory.builder()
    .let { builder ->
      builder.buildRelease(
        offenderBooking = offenderBooking,
        date = date,
      )
        .also { offenderBooking.externalMovements.forEach { it.active = false } }
        .also { offenderBooking.externalMovements += it }
    }

  override fun receive(
    date: LocalDateTime,
  ): OffenderExternalMovement = offenderExternalMovementBuilderFactory.builder()
    .let { builder ->
      builder.buildReceive(
        offenderBooking = offenderBooking,
        date = date,
      )
        .also { offenderBooking.externalMovements += it }
    }

  override fun temporaryAbsence(
    date: LocalDateTime,
    fromPrison: String,
    toAgency: String?,
    movementReason: String,
    arrestAgency: String?,
    escort: String?,
    escortText: String?,
    comment: String?,
    toCity: String?,
    toAddress: Address?,
  ): OffenderTemporaryAbsence = offenderExternalMovementBuilderFactory.builder()
    .buildTemporaryAbsence(
      offenderBooking = offenderBooking,
      date = date,
      fromPrison = fromPrison,
      toAgency = toAgency,
      movementReason = movementReason,
      arrestAgency = arrestAgency,
      escort = escort,
      escortText = escortText,
      comment = comment,
      toCity = toCity,
      toAddress = toAddress,
    )
    .also { offenderBooking.externalMovements += it }

  override fun temporaryAbsenceReturn(
    date: LocalDateTime,
    fromAgency: String?,
    toPrison: String,
    movementReason: String,
    escort: String?,
    escortText: String?,
    comment: String?,
    fromCity: String?,
    fromAddress: Address?,
  ): OffenderTemporaryAbsenceReturn = offenderExternalMovementBuilderFactory.builder()
    .buildTemporaryAbsenceReturn(
      offenderBooking = offenderBooking,
      date = date,
      fromAgency = fromAgency,
      toPrison = toPrison,
      movementReason = movementReason,
      escort = escort,
      escortText = escortText,
      comment = comment,
      fromCity = fromCity,
      fromAddress = fromAddress,
    )
    .also { offenderBooking.externalMovements += it }

  override fun visitBalance(
    remainingVisitOrders: Int?,
    remainingPrivilegedVisitOrders: Int?,
    dsl: VisitBalanceDsl.() -> Unit,
  ): OffenderVisitBalance = visitBalanceBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offenderBooking = offenderBooking,
        remainingVisitOrders = remainingVisitOrders,
        remainingPrivilegedVisitOrders = remainingPrivilegedVisitOrders,
      ).also {
        builder.apply(dsl)
      }
    }

  override fun visit(
    visitTypeCode: String,
    visitStatusCode: String,
    startDateTimeString: String,
    endDateTimeString: String,
    agyLocId: String,
    agencyInternalLocationDescription: String?,
    createdDatetime: LocalDateTime?,
    dsl: VisitDsl.() -> Unit,
  ): Visit = visitBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offenderBooking = offenderBooking,
        visitTypeCode = visitTypeCode,
        visitStatusCode = visitStatusCode,
        startDateTimeString = startDateTimeString,
        endDateTimeString = endDateTimeString,
        agyLocId = agyLocId,
        agencyInternalLocationDescription = agencyInternalLocationDescription,
        visitSlot = null,
        createdDatetime = createdDatetime,
      ).also {
        offenderBooking.visits += it
        builder.apply(dsl)
      }
    }
  override fun visitOrder(
    orderNumber: Long,
    visitOrderTypeCode: String,
    visitStatusCode: String,
    issueDate: LocalDate,
    dsl: VisitOrderDsl.() -> Unit,
  ): VisitOrder = visitOrderBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offenderBooking = offenderBooking,
        visitOrderTypeCode = visitOrderTypeCode,
        visitStatusCode = visitStatusCode,
        orderNumber = orderNumber,
        issueDate = issueDate,
      ).also {
        builder.apply(dsl)
      }
    }
  override fun officialVisit(
    visitTypeCode: String,
    visitStatusCode: String,
    visitDate: LocalDate,
    visitSlot: AgencyVisitSlot,
    comment: String?,
    visitorConcern: String?,
    overrideBanStaff: Staff?,
    prisonerSearchTypeCode: String?,
    visitOrder: VisitOrder?,
    dsl: VisitDsl.() -> Unit,
  ): Visit = visitBuilderFactory.builder()
    .let { builder ->
      builder.build(
        offenderBooking = offenderBooking,
        visitTypeCode = visitTypeCode,
        visitStatusCode = visitStatusCode,
        startDateTimeString = visitDate.atTime(visitSlot.agencyVisitTime.startTime).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        endDateTimeString = visitDate.atTime(visitSlot.agencyVisitTime.endTime).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        agyLocId = visitSlot.location.id,
        agencyInternalLocationDescription = visitSlot.agencyInternalLocation.description,
        visitSlot = visitSlot,
        comment = comment,
        visitorConcern = visitorConcern,
        overrideBanStaff = overrideBanStaff,
        prisonerSearchTypeCode = prisonerSearchTypeCode,
        visitOrder = visitOrder,
      ).also {
        offenderBooking.visits += it
        builder.apply(dsl)
      }
    }

  override fun restriction(
    restrictionType: String,
    enteredStaff: Staff,
    authorisedStaff: Staff,
    comment: String?,
    effectiveDate: LocalDate,
    expiryDate: LocalDate?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: OffenderRestrictionsDsl.() -> Unit,
  ): OffenderRestrictions = offenderRestrictionsBuilderFactory.builder().let { builder ->
    builder.build(
      offenderBooking = offenderBooking,
      restrictionType = restrictionType,
      enteredStaff = enteredStaff,
      authorisedStaff = authorisedStaff,
      comment = comment,
      effectiveDate = effectiveDate,
      expiryDate = expiryDate,
      whenCreated = whenCreated,
      whoCreated = whoCreated,
    )
      .also { offenderBooking.restrictions += it }
      .also { builder.apply(dsl) }
  }

  override fun contact(
    person: Person,
    relationshipType: String,
    contactType: String,
    active: Boolean,
    nextOfKin: Boolean,
    emergencyContact: Boolean,
    approvedVisitor: Boolean,
    comment: String?,
    expiryDate: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: OffenderContactPersonDsl.() -> Unit,
  ): OffenderContactPerson = offenderContactPersonBuilderFactory.builder().let { builder ->
    builder.build(
      offenderBooking = offenderBooking,
      person = person,
      relationshipType = relationshipType,
      contactType = contactType,
      active = active,
      nextOfKin = nextOfKin,
      emergencyContact = emergencyContact,
      approvedVisitor = approvedVisitor,
      comment = comment,
      expiryDate = expiryDate?.let { LocalDate.parse(it) },
      whenCreated = whenCreated,
      whoCreated = whoCreated,
    )
      .also { offenderBooking.contacts += it }
      .also { builder.apply(dsl) }
  }

  override fun adjustment(
    adjustmentTypeCode: String,
    adjustmentDate: LocalDate,
    createdDate: LocalDateTime,
    adjustmentNumberOfDays: Long,
    active: Boolean,
    dsl: OffenderKeyDateAdjustmentDsl.() -> Unit,
  ): OffenderKeyDateAdjustment = offenderKeyDateAdjustmentBuilderFactory.builder().let { builder ->
    builder.build(
      adjustmentTypeCode = adjustmentTypeCode,
      adjustmentDate = adjustmentDate,
      createdDate = createdDate,
      adjustmentNumberOfDays = adjustmentNumberOfDays,
      active = active,
      offenderBooking = offenderBooking,
    )
      .also { offenderBooking.keyDateAdjustments += it }
      .also { builder.apply(dsl) }
  }

  override fun incentive(
    iepLevelCode: String,
    userId: String?,
    sequence: Long,
    commentText: String,
    auditModuleName: String?,
    iepDateTime: LocalDateTime,
  ) = incentiveBuilderFactory.builder()
    .build(
      offenderBooking,
      iepLevelCode,
      userId,
      sequence,
      commentText,
      auditModuleName,
      iepDateTime,
    )
    .also { offenderBooking.incentives += it }

  override fun adjudicationParty(
    incident: AdjudicationIncident,
    comment: String,
    role: PartyRole,
    partyAddedDate: LocalDate,
    staff: Staff?,
    adjudicationNumber: Long?,
    actionDecision: String,
    dsl: AdjudicationPartyDsl.() -> Unit,
  ) = adjudicationPartyBuilderFactory.builder().let { builder ->
    builder.build(
      adjudicationNumber = adjudicationNumber,
      comment = comment,
      staff = staff,
      incidentRole = role.code,
      actionDecision = actionDecision,
      partyAddedDate = partyAddedDate,
      incident = incident,
      offenderBooking = offenderBooking,
      whenCreated = LocalDateTime.now(),
      index = incident.parties.size + 1,
    )
      .also { incident.parties += it }
      .also { builder.apply(dsl) }
  }

  override fun document(
    fileName: String,
    template: IWPTemplate,
    status: String,
    body: String?,
    dsl: IWPDocumentDsl.() -> Unit,
  ) = documentBuilderFactory.builder().let { builder ->
    builder.build(
      fileName = fileName,
      offenderBooking = offenderBooking,
      status = status,
      template = template,
      body = body,
    )
      .also { offenderBooking.documents += it }
      .also { builder.apply(dsl) }
  }

  override fun image(
    captureDateTime: LocalDateTime,
    fullSizeImage: ByteArray,
    thumbnailImage: ByteArray,
    active: Boolean,
    imageSourceCode: String,
    dsl: OffenderBookingImageDsl.() -> Unit,
  ): OffenderBookingImage = offenderBookingImageBuilderFactory.builder().let { builder ->
    builder.build(
      offenderBooking = offenderBooking,
      captureDateTime = captureDateTime,
      fullSizeImage = fullSizeImage,
      thumbnailImage = thumbnailImage,
      active = active,
      imageSourceCode = imageSourceCode,
    )
      .also { offenderBooking.images += it }
      .also { builder.apply(dsl) }
  }

  override fun identifyingMark(
    sequence: Long,
    bodyPartCode: String,
    markTypeCode: String,
    sideCode: String?,
    partOrientationCode: String?,
    commentText: String?,
    dsl: OffenderIdentifyingMarkDsl.() -> Unit,
  ): OffenderIdentifyingMark = offenderIdentifyingMarkBuilderFactory.builder().let { builder ->
    builder.build(
      offenderBooking = offenderBooking,
      sequence = sequence,
      bodyPartCode = bodyPartCode,
      markTypeCode = markTypeCode,
      sideCode = sideCode,
      partOrientationCode = partOrientationCode,
      commentText = commentText,
    )
      .also { offenderBooking.identifyingMarks += it }
      .also { builder.apply(dsl) }
  }

  override fun belief(
    beliefCode: String,
    startDate: LocalDate,
    endDate: LocalDate?,
    changeReason: Boolean?,
    comments: String?,
    verified: Boolean?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: OffenderBeliefDsl.() -> Unit,
  ): OffenderBelief = offenderBeliefBuilderFactory.builder().let { builder ->
    builder.build(
      booking = offenderBooking,
      offender = offenderBooking.offender,
      beliefCode = beliefCode,
      startDate = startDate,
      endDate = endDate,
      changeReason = changeReason,
      comments = comments,
      verified = verified,
      whenCreated = whenCreated,
      whoCreated = whoCreated,
    )
      .also { builder.apply(dsl) }
  }

  override fun transaction(
    transactionId: Long,
    transactionEntrySequence: Int,
    transactionType: String,
    entryDate: LocalDate,
    dsl: OffenderTransactionDsl.() -> Unit,
  ): OffenderTransaction = offenderTransactionBuilderFactory.builder().let { builder ->
    builder.build(
      transactionId,
      transactionEntrySequence,
      offenderBooking,
      offenderBooking.offender,
      offenderBooking.location.id,
      transactionType,
      entryDate,
    )
      .also { builder.apply(dsl) }
  }

  override fun temporaryAbsenceApplication(
    eventSubType: String,
    applicationDate: LocalDateTime,
    applicationTime: LocalDateTime,
    fromDate: LocalDate,
    releaseTime: LocalDateTime,
    toDate: LocalDate,
    returnTime: LocalDateTime,
    applicationStatus: String,
    escort: String?,
    transportType: String?,
    comment: String?,
    toAddress: Address?,
    prison: String,
    toAgency: String?,
    contactPersonName: String?,
    applicationType: String,
    temporaryAbsenceType: String?,
    temporaryAbsenceSubType: String?,
    dsl: OffenderTemporaryAbsenceApplicationDsl.() -> Unit,
  ): OffenderMovementApplication = offenderTemporaryAbsenceApplicationBuilderFactory.builder().let { builder ->
    builder.build(
      offenderBooking = offenderBooking,
      eventSubType = eventSubType,
      applicationDate = applicationDate,
      applicationTime = applicationTime,
      fromDate = fromDate,
      releaseTime = releaseTime,
      toDate = toDate,
      returnTime = returnTime,
      applicationStatus = applicationStatus,
      escort = escort,
      transportType = transportType,
      comment = comment,
      toAddress = toAddress,
      prison = prison,
      toAgency = toAgency,
      contactPersonName = contactPersonName,
      applicationType = applicationType,
      temporaryAbsenceType = temporaryAbsenceType,
      temporaryAbsenceSubType = temporaryAbsenceSubType,
    )
      .also { offenderBooking.temporaryAbsenceApplications += it }
      .also { builder.apply(dsl) }
  }
}
