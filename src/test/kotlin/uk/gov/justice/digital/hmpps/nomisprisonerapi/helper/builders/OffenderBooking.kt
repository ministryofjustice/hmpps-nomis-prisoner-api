package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.WITNESS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPDocument
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPTemplate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NoteSourceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlert
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseNote
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderKeyDateAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributes
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DslMarker
annotation class BookingDslMarker

@NomisDataDslMarker
interface BookingDsl {
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
    createUsername: String = "FRED.JAMES",
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
    courtOrder: Long? = null,
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
    prisonId: String = "COURT1",
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
    caseNoteText: String?,
    amendmentFlag: Boolean = false,
    noteSourceCode: NoteSourceCode = NoteSourceCode.INST,
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
}

@Component
class BookingBuilderRepository(
  private val offenderBookingRepository: OffenderBookingRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
) {
  fun save(offenderBooking: OffenderBooking): OffenderBooking = offenderBookingRepository.save(offenderBooking)
  fun lookupAgencyLocation(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
  fun lookupAgencyInternalLocation(id: Long): AgencyInternalLocation =
    agencyInternalLocationRepository.findByIdOrNull(id)!!
}

@Component
class BookingBuilderFactory(
  private val repository: BookingBuilderRepository,
  private val courseAllocationBuilderFactory: CourseAllocationBuilderFactory,
  private val incentiveBuilderFactory: IncentiveBuilderFactory,
  private val adjudicationPartyBuilderFactory: AdjudicationPartyBuilderFactory,
  private val offenderSentenceBuilderFactory: OffenderSentenceBuilderFactory,
  private val courtCaseBuilderFactory: CourtCaseBuilderFactory,
  private val offenderKeyDateAdjustmentBuilderFactory: OffenderKeyDateAdjustmentBuilderFactory,
  private val offenderExternalMovementBuilderFactory: OffenderExternalMovementBuilderFactory,
  private val offenderAlertBuilderFactory: OffenderAlertBuilderFactory,
  private val offenderCaseNoteBuilderFactory: OffenderCaseNoteBuilderFactory,
  private val csipReportBuilderFactory: CSIPReportBuilderFactory,
  private val documentBuilderFactory: IWPDocumentBuilderFactory,
  private val offenderPhysicalAttributesBuilderFactory: OffenderPhysicalAttributesBuilderFactory,
) {
  fun builder() = BookingBuilder(
    repository,
    courseAllocationBuilderFactory,
    incentiveBuilderFactory,
    adjudicationPartyBuilderFactory,
    offenderSentenceBuilderFactory,
    courtCaseBuilderFactory,
    offenderKeyDateAdjustmentBuilderFactory,
    offenderExternalMovementBuilderFactory,
    offenderAlertBuilderFactory,
    offenderCaseNoteBuilderFactory,
    csipReportBuilderFactory,
    documentBuilderFactory,
    offenderPhysicalAttributesBuilderFactory,
  )
}

class BookingBuilder(
  private val repository: BookingBuilderRepository,
  private val courseAllocationBuilderFactory: CourseAllocationBuilderFactory,
  private val incentiveBuilderFactory: IncentiveBuilderFactory,
  private val adjudicationPartyBuilderFactory: AdjudicationPartyBuilderFactory,
  private val offenderSentenceBuilderFactory: OffenderSentenceBuilderFactory,
  private val courtCaseBuilderFactory: CourtCaseBuilderFactory,
  private val offenderKeyDateAdjustmentBuilderFactory: OffenderKeyDateAdjustmentBuilderFactory,
  private val offenderExternalMovementBuilderFactory: OffenderExternalMovementBuilderFactory,
  private val offenderAlertBuilderFactory: OffenderAlertBuilderFactory,
  private val offenderCaseNoteBuilderFactory: OffenderCaseNoteBuilderFactory,
  private val csipReportBuilderFactory: CSIPReportBuilderFactory,
  private val documentBuilderFactory: IWPDocumentBuilderFactory,
  private val offenderPhysicalAttributesBuilderFactory: OffenderPhysicalAttributesBuilderFactory,
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
    dsl: CourseAllocationDsl.() -> Unit,
  ) =
    courseAllocationBuilderFactory.builder()
      .let { builder ->
        builder.build(
          offenderBooking,
          startDate,
          programStatusCode,
          endDate,
          endReasonCode,
          endComment,
          suspended,
          courseActivity,
        )
          .also { offenderBooking.offenderProgramProfiles += it }
          .also { builder.apply(dsl) }
      }

  override fun sentence(
    calculationType: String,
    category: String,
    startDate: LocalDate,
    status: String,
    sentenceLevel: String,
    consecSequence: Int?,
    courtOrder: Long?,
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
  ): OffenderSentence =
    offenderSentenceBuilderFactory.builder()
      .let { builder ->
        builder.build(
          calculationType = calculationType,
          category = category,
          startDate = startDate,
          status = status,
          offenderBooking = offenderBooking,
          sequence = offenderBooking.sentences.size.toLong() + 1,
          sentenceLevel = sentenceLevel,
          consecSequence = consecSequence,
          // todo
          courtOrder = null,
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
    createUsername: String,
    dsl: CSIPReportDsl.() -> Unit,
  ): CSIPReport =
    csipReportBuilderFactory.builder()
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
          createUsername = createUsername,
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
    caseNoteText: String?,
    amendmentFlag: Boolean,
    noteSourceCode: NoteSourceCode,
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

  override fun prisonTransfer(
    from: String,
    to: String,
    date: LocalDateTime,
  ): Pair<OffenderExternalMovement, OffenderExternalMovement> =
    offenderExternalMovementBuilderFactory.builder()
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
  ): OffenderExternalMovement =
    offenderExternalMovementBuilderFactory.builder()
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
  ): OffenderExternalMovement =
    offenderExternalMovementBuilderFactory.builder()
      .let { builder ->
        builder.buildReceive(
          offenderBooking = offenderBooking,
          date = date,
        )
          .also { offenderBooking.externalMovements += it }
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
  ) =
    incentiveBuilderFactory.builder()
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
  ) =
    adjudicationPartyBuilderFactory.builder().let { builder ->
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
  ) =
    documentBuilderFactory.builder().let { builder ->
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
}
