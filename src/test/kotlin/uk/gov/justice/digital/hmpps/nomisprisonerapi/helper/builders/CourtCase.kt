package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LegalCaseType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class CourtCaseDslMarker

@NomisDataDslMarker
interface CourtCaseDsl {
  @CourtEventDslMarker
  fun courtEvent(
    commentText: String? = "Court event comment",
    prison: String = "MDI",
    courtEventType: String = "TRIAL",
    eventStatusCode: String = "SCH",
    outcomeReasonCode: String? = "1046",
    judgeName: String? = "Mike",
    directionCode: String? = "IN",
    eventDate: LocalDate = LocalDate.of(2023, 1, 1),
    startTime: LocalDateTime = LocalDateTime.of(2023, 1, 1, 10, 30),
    nextEventStartTime: LocalDateTime? = LocalDateTime.of(2023, 1, 5, 10, 30),
    nextEventDate: LocalDate? = LocalDate.of(2023, 1, 5),
    nextEventRequestFlag: Boolean? = false,
    orderRequestedFlag: Boolean? = false,
    holdFlag: Boolean? = false,
    dsl: CourtEventDsl.() -> Unit = {},
  ): CourtEvent

  @OffenderChargeDslMarker
  fun offenderCharge(
    offenceDate: LocalDate = LocalDate.of(2023, 1, 1),
    offenceEndDate: LocalDate = LocalDate.of(2023, 1, 2),
    offenceCode: String = "RC86354",
    statuteCode: String = "RC86",
    offencesCount: Int? = 1,
    cjitCode1: String? = "cjit1",
    cjitCode2: String? = "cjit2",
    cjitCode3: String? = "cjit3",
    resultCode1: String? = "1002",
    resultCode2: String? = "1003",
    resultCode1Indicator: String? = "rci1",
    resultCode2Indicator: String? = "rci2",
    mostSeriousFlag: Boolean = false,
    chargeStatus: String? = "A",
    propertyValue: BigDecimal? = BigDecimal(3.2),
    totalPropertyValue: BigDecimal? = BigDecimal(10),
    plea: String? = "G",
    lidsOffenceNumber: Int? = 1,
    dsl: OffenderChargeDsl.() -> Unit = {},
  ): OffenderCharge
}

@Component
class CourtCaseBuilderFactory(
  private val repository: CourtCaseBuilderRepository,
  private val courtEventBuilderFactory: CourtEventBuilderFactory,
  private val offenderChargeBuilderFactory: OffenderChargeBuilderFactory,
) {
  fun builder(): CourtCaseBuilder {
    return CourtCaseBuilder(repository, courtEventBuilderFactory, offenderChargeBuilderFactory)
  }
}

@Component
class CourtCaseBuilderRepository(
  private val repository: CourtCaseRepository,
  private val legalCaseTypeRepository: ReferenceCodeRepository<LegalCaseType>,
  private val caseStatusRepository: ReferenceCodeRepository<CaseStatus>,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun save(courtCase: CourtCase): CourtCase =
    repository.save(courtCase)

  fun lookupCaseType(code: String): LegalCaseType =
    legalCaseTypeRepository.findByIdOrNull(LegalCaseType.pk(code))!!

  fun lookupCaseStatus(code: String): CaseStatus =
    caseStatusRepository.findByIdOrNull(CaseStatus.pk(code))!!

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
}

class CourtCaseBuilder(
  private val repository: CourtCaseBuilderRepository,
  private val courtEventBuilderFactory: CourtEventBuilderFactory,
  private val offenderChargeBuilderFactory: OffenderChargeBuilderFactory,
) : CourtCaseDsl {
  private lateinit var courtCase: CourtCase
  private lateinit var whenCreated: LocalDateTime

  fun build(
    offenderBooking: OffenderBooking,
    whenCreated: LocalDateTime,
    caseInfoNumber: String?,
    caseSequence: Int,
    caseStatus: String,
    caseType: String,
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
    caseInfoNumber = caseInfoNumber,
    caseSequence = caseSequence,
    caseStatus = repository.lookupCaseStatus(caseStatus),
    legalCaseType = repository.lookupCaseType(caseType),
    offenderBooking = offenderBooking,
    prison = repository.lookupAgency(prisonId),
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
    statuteCode: String,
    offencesCount: Int?,
    cjitCode1: String?,
    cjitCode2: String?,
    cjitCode3: String?,
    resultCode1: String?,
    resultCode2: String?,
    resultCode1Indicator: String?,
    resultCode2Indicator: String?,
    mostSeriousFlag: Boolean,
    chargeStatus: String?,
    propertyValue: BigDecimal?,
    totalPropertyValue: BigDecimal?,
    plea: String?,
    lidsOffenceNumber: Int?,
    dsl: OffenderChargeDsl.() -> Unit,
  ) =
    offenderChargeBuilderFactory.builder().let { builder ->
      builder.build(
        offenderBooking = courtCase.offenderBooking,
        courtCase = courtCase,
        offenceDate = offenceDate,
        offenceEndDate = offenceEndDate,
        offenceCode = offenceCode,
        statuteCode = statuteCode,
        offencesCount = offencesCount,
        cjitCode1 = cjitCode1,
        cjitCode2 = cjitCode2,
        cjitCode3 = cjitCode3,
        resultCode1 = resultCode1,
        resultCode2 = resultCode2,
        resultCode1Indicator = resultCode1Indicator,
        resultCode2Indicator = resultCode2Indicator,
        mostSeriousFlag = mostSeriousFlag,
        chargeStatus = chargeStatus,
        lidsOffenceNumber = lidsOffenceNumber,
        propertyValue = propertyValue,
        totalPropertyValue = totalPropertyValue,
        plea = plea,
      )
        // .also { courtCase.courtEvents += it }
        .also { builder.apply(dsl) }
    }

  override fun courtEvent(
    commentText: String?,
    prison: String,
    courtEventType: String,
    eventStatusCode: String,
    outcomeReasonCode: String?,
    judgeName: String?,
    directionCode: String?,
    eventDate: LocalDate,
    startTime: LocalDateTime,
    nextEventStartTime: LocalDateTime?,
    nextEventDate: LocalDate?,
    nextEventRequestFlag: Boolean?,
    orderRequestedFlag: Boolean?,
    holdFlag: Boolean?,
    dsl: CourtEventDsl.() -> Unit,
  ) =
    courtEventBuilderFactory.builder().let { builder ->
      builder.build(
        commentText = commentText,
        prison = prison,
        courtEventType = courtEventType,
        eventStatusCode = eventStatusCode,
        outcomeReasonCode = outcomeReasonCode,
        judgeName = judgeName,
        directionCode = directionCode,
        eventDate = eventDate,
        startTime = startTime,
        nextEventStartTime = nextEventStartTime,
        nextEventDate = nextEventDate,
        offenderBooking = courtCase.offenderBooking,
        courtCase = courtCase,
        nextEventRequestFlag = nextEventRequestFlag,
        orderRequestedFlag = orderRequestedFlag,
        holdFlag = holdFlag,
      )
        .also { courtCase.courtEvents += it }
        .also { builder.apply(dsl) }
    }
}
