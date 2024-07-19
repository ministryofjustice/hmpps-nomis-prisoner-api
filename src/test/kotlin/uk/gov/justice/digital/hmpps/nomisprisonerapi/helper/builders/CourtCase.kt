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

  @OffenderChargeDslMarker
  fun offenderCharge(
    offenceDate: LocalDate = LocalDate.of(2023, 1, 1),
    offenceEndDate: LocalDate = LocalDate.of(2023, 1, 5),
    offenceCode: String = "RR84700",
    offencesCount: Int? = 1,
    cjitCode1: String? = "cj6",
    cjitCode2: String? = "cj7",
    cjitCode3: String? = "cj8",
    resultCode1: String? = "1005",
    resultCode2: String? = "1006",
    mostSeriousFlag: Boolean = true,
    propertyValue: BigDecimal? = BigDecimal(8.3),
    totalPropertyValue: BigDecimal? = BigDecimal(11),
    plea: String? = "G",
    lidsOffenceNumber: Int? = 11,
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
  private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
  fun save(courtCase: CourtCase): CourtCase =
    repository.saveAndFlush(courtCase)

  fun lookupCaseType(code: String): LegalCaseType =
    legalCaseTypeRepository.findByIdOrNull(LegalCaseType.pk(code))!!

  fun lookupCaseStatus(code: String): CaseStatus =
    caseStatusRepository.findByIdOrNull(CaseStatus.pk(code))!!

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
    caseInfoNumber = caseInfoNumber,
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
  ) =
    offenderChargeBuilderFactory.builder().let { builder ->
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
  ) =
    courtEventBuilderFactory.builder().let { builder ->
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

  override fun audit(
    createDatetime: LocalDateTime,
  ) = repository.updateAudit(
    id = courtCase.id,
    createDatetime = createDatetime,
  )
}
