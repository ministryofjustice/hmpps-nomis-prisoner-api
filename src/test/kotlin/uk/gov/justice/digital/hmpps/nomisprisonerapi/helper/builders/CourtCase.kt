package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LegalCaseType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtCaseRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository
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
}

@Component
class CourtCaseBuilderFactory(
  private val repository: CourtCaseBuilderRepository,
  private val courtEventBuilderFactory: CourtEventBuilderFactory,
) {
  fun builder(): CourtCaseBuilder {
    return CourtCaseBuilder(repository, courtEventBuilderFactory)
  }
}

@Component
class CourtCaseBuilderRepository(
  private val repository: CourtCaseRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val legalCaseTypeRepository: ReferenceCodeRepository<LegalCaseType>,
  private val caseStatusRepository: ReferenceCodeRepository<CaseStatus>,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val staffRepository: StaffRepository,
) {
  fun save(courtCase: CourtCase): CourtCase =
    repository.save(courtCase)

  fun lookupAgencyInternalLocation(locationId: Long): AgencyInternalLocation? =
    agencyInternalLocationRepository.findByIdOrNull(locationId)

  fun lookupCaseType(code: String): LegalCaseType =
    legalCaseTypeRepository.findByIdOrNull(LegalCaseType.pk(code))!!

  fun lookupCaseStatus(code: String): CaseStatus =
    caseStatusRepository.findByIdOrNull(CaseStatus.pk(code))!!

  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!

  fun lookupStaff(id: Long): Staff =
    staffRepository.findByIdOrNull(id)!!
}

class CourtCaseBuilder(
  private val repository: CourtCaseBuilderRepository,
  private val courtEventBuilderFactory: CourtEventBuilderFactory,
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
