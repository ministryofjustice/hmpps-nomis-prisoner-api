package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LegalCaseType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class CourtCaseDslMarker

@DslMarker
annotation class CourtCaseAuditDslMarker

@NomisDataDslMarker
interface CourtCaseDsl {
  @CourtEventDslMarker
  fun courtEvent(
    commentText: String? = "Court event comment",
    prison: String = "MDI",
    courtEventType: String = "TRIAL",
    eventStatusCode: String = "SCH",
    outcomeReasonCode: String? = "3514",
    judgeName: String? = "Mike",
    eventDateTime: LocalDateTime = LocalDateTime.of(2023, 1, 1, 10, 30),
    nextEventDateTime: LocalDateTime? = LocalDateTime.of(2023, 1, 5, 10, 30),
    orderRequestedFlag: Boolean? = false,
    dsl: CourtEventDsl.() -> Unit = {},
  ): CourtEvent

  @CourtCaseAuditDslMarker
  fun audit(
    createDatetime: LocalDateTime = LocalDateTime.now(),
  )

  @OffenderCaseIdentifierDslMarker
  fun offenderCaseIdentifier(
    reference: String = "caseRef1",
    type: String = "CASE/INFO#",
    dsl: OffenderCaseIdentifierDsl.() -> Unit = {},
  ): OffenderCaseIdentifier

  @OffenderChargeDslMarker
  fun offenderCharge(
    offenceDate: LocalDate = LocalDate.of(2023, 1, 1),
    offenceEndDate: LocalDate = LocalDate.of(2023, 1, 5),
    offenceCode: String = "RR84700",
    offencesCount: Int? = null,
    cjitCode1: String? = "cj6",
    cjitCode2: String? = "cj7",
    cjitCode3: String? = "cj8",
    resultCode1: String? = "1005",
    resultCode2: String? = "1006",
    mostSeriousFlag: Boolean = true,
    propertyValue: BigDecimal? = null,
    totalPropertyValue: BigDecimal? = null,
    plea: String? = "G",
    lidsOffenceNumber: Int? = 11,
    dsl: OffenderChargeDsl.() -> Unit = {},
  ): OffenderCharge

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
}

@Component
class CourtCaseBuilderFactory(
  private val repository: CourtCaseBuilderRepository,
  private val courtEventBuilderFactory: CourtEventBuilderFactory,
  private val offenderChargeBuilderFactory: OffenderChargeBuilderFactory,
  private val offenderCaseIdentifierBuilderFactory: OffenderCaseIdentifierBuilderFactory,
  private val offenderSentenceBuilderFactory: OffenderSentenceBuilderFactory,
) {
  fun builder(): CourtCaseBuilder = CourtCaseBuilder(repository, courtEventBuilderFactory, offenderChargeBuilderFactory, offenderCaseIdentifierBuilderFactory, offenderSentenceBuilderFactory)
}

@Component
class CourtCaseBuilderRepository(
  private val repository: CourtCaseRepository,
  private val legalCaseTypeRepository: ReferenceCodeRepository<LegalCaseType>,
  private val caseStatusRepository: ReferenceCodeRepository<CaseStatus>,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
  fun save(courtCase: CourtCase): CourtCase = repository.saveAndFlush(courtCase)

  fun lookupCaseType(code: String): LegalCaseType = legalCaseTypeRepository.findByIdOrNull(LegalCaseType.pk(code))!!

  fun lookupCaseStatus(code: String): CaseStatus = caseStatusRepository.findByIdOrNull(CaseStatus.pk(code))!!

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!

  fun updateAudit(
    id: Long,
    createDatetime: LocalDateTime,
  ) {
    jdbcTemplate.update(
      """
      UPDATE OFFENDER_CASES 
      SET 
        CREATE_DATETIME = :createDatetime 
      WHERE CASE_ID = :id 
      """,
      mapOf(
        "createDatetime" to createDatetime,
        "id" to id,
      ),
    )
  }
}

class CourtCaseBuilder(
  private val repository: CourtCaseBuilderRepository,
  private val courtEventBuilderFactory: CourtEventBuilderFactory,
  private val offenderChargeBuilderFactory: OffenderChargeBuilderFactory,
  private val offenderCaseIdentifierBuilderFactory: OffenderCaseIdentifierBuilderFactory,
  private val offenderSentenceBuilderFactory: OffenderSentenceBuilderFactory,
) : CourtCaseDsl {
  private lateinit var courtCase: CourtCase
  private lateinit var whenCreated: LocalDateTime

  fun build(
    offenderBooking: OffenderBooking,
    whenCreated: LocalDateTime,
    caseInfoNumber: String?,
    caseSequence: Int,
    caseStatus: String,
    legalCaseType: String,
    beginDate: LocalDate,
    prisonId: String,
    combinedCase: CourtCase?,
    statusUpdateReason: String?,
    statusUpdateComment: String?,
    statusUpdateDate: LocalDate?,
    statusUpdateStaff: Staff?,
    lidsCaseId: Int?,
    lidsCaseNumber: Int,
    lidsCombinedCaseId: Int?,
  ): CourtCase = CourtCase(
    beginDate = beginDate,
    primaryCaseInfoNumber = caseInfoNumber,
    caseSequence = caseSequence,
    caseStatus = repository.lookupCaseStatus(caseStatus),
    legalCaseType = repository.lookupCaseType(legalCaseType),
    offenderBooking = offenderBooking,
    court = repository.lookupAgency(prisonId),
    combinedCase = combinedCase,
    statusUpdateStaff = statusUpdateStaff,
    statusUpdateDate = statusUpdateDate,
    statusUpdateComment = statusUpdateComment,
    statusUpdateReason = statusUpdateReason,
    lidsCaseId = lidsCaseId,
    lidsCombinedCaseId = lidsCombinedCaseId,
    lidsCaseNumber = lidsCaseNumber,
  )
    .let { repository.save(it) }
    .also { courtCase = it }
    .also { this.whenCreated = whenCreated }

  override fun offenderCharge(
    offenceDate: LocalDate,
    offenceEndDate: LocalDate,
    offenceCode: String,
    offencesCount: Int?,
    cjitCode1: String?,
    cjitCode2: String?,
    cjitCode3: String?,
    resultCode1: String?,
    resultCode2: String?,
    mostSeriousFlag: Boolean,
    propertyValue: BigDecimal?,
    totalPropertyValue: BigDecimal?,
    plea: String?,
    lidsOffenceNumber: Int?,
    dsl: OffenderChargeDsl.() -> Unit,
  ) = offenderChargeBuilderFactory.builder().let { builder ->
    builder.build(
      offenderBooking = courtCase.offenderBooking,
      courtCase = courtCase,
      offenceDate = offenceDate,
      offenceEndDate = offenceEndDate,
      offenceCode = offenceCode,
      offencesCount = offencesCount,
      cjitCode1 = cjitCode1,
      cjitCode2 = cjitCode2,
      cjitCode3 = cjitCode3,
      resultCode1 = resultCode1,
      resultCode2 = resultCode2,
      mostSeriousFlag = mostSeriousFlag,
      lidsOffenceNumber = lidsOffenceNumber,
      propertyValue = propertyValue,
      totalPropertyValue = totalPropertyValue,
      plea = plea,
    )
      .also { courtCase.offenderCharges += it }
      .also { builder.apply(dsl) }
  }

  override fun offenderCaseIdentifier(
    reference: String,
    type: String,
    dsl: OffenderCaseIdentifierDsl.() -> Unit,
  ) = offenderCaseIdentifierBuilderFactory.builder().let { builder ->
    builder.build(
      reference = reference,
      type = type,
      courtCase = courtCase,
    )
      .also { courtCase.caseInfoNumbers += it }
      .also { builder.apply(dsl) }
  }

  override fun courtEvent(
    commentText: String?,
    prison: String,
    courtEventType: String,
    eventStatusCode: String,
    outcomeReasonCode: String?,
    judgeName: String?,
    eventDateTime: LocalDateTime,
    nextEventDateTime: LocalDateTime?,
    orderRequestedFlag: Boolean?,
    dsl: CourtEventDsl.() -> Unit,
  ) = courtEventBuilderFactory.builder().let { builder ->
    builder.build(
      commentText = commentText,
      prison = prison,
      courtEventType = courtEventType,
      eventStatusCode = eventStatusCode,
      outcomeReasonCode = outcomeReasonCode,
      judgeName = judgeName,
      eventDateTime = eventDateTime,
      nextEventDateTime = nextEventDateTime,
      offenderBooking = courtCase.offenderBooking,
      courtCase = courtCase,
      orderRequestedFlag = orderRequestedFlag,
    )
      .also { courtCase.courtEvents += it }
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
  ): OffenderSentence = offenderSentenceBuilderFactory.builder()
    .let { builder ->
      builder.build(
        calculationType = calculationType,
        category = category,
        startDate = startDate,
        status = status,
        offenderBooking = courtCase.offenderBooking,
        sequence = courtCase.offenderBooking.sentences.size.toLong() + 1,
        sentenceLevel = sentenceLevel,
        consecLineSequence = consecSequence,
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
        .also { courtCase.sentences += it }
        .also { courtCase.offenderBooking.sentences += it }
        .also { builder.apply(dsl) }
    }

  override fun audit(
    createDatetime: LocalDateTime,
  ) = repository.updateAudit(
    id = courtCase.id,
    createDatetime = createDatetime,
  )
}
