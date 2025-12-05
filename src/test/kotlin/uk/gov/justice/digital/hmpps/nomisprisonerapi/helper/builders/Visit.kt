package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SearchLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@DslMarker
annotation class VisitDslMarker

@NomisDataDslMarker
interface VisitDsl {
  @VisitVisitorDslMarker
  fun visitor(
    person: Person,
    groupLeader: Boolean = false,
    assistedVisit: Boolean = false,
    outcomeReasonCode: String? = null,
    eventOutcomeCode: String = "ATT",
    eventStatusCode: String = "COMP",
    comment: String? = null,
    dsl: VisitVisitorDsl.() -> Unit = {},
  ): VisitVisitor

  @VisitOutcomeDslMarker
  fun visitOutcome(
    outcomeReasonCode: String,
    eventOutcomeCode: String = "ATT",
    eventStatusCode: String = "COMP",
    dsl: VisitOutcomeDsl.() -> Unit = {},
  ): VisitVisitor
}

@Component
class VisitBuilderRepository(
  private val visitRepository: VisitRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val visitStatusRepository: ReferenceCodeRepository<VisitStatus>,
  private val visitTypeRepository: ReferenceCodeRepository<VisitType>,
  private val searchLevelRepository: ReferenceCodeRepository<SearchLevel>,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(visit: Visit): Visit = visitRepository.saveAndFlush(visit)
  fun updateCreateDatetime(visit: Visit, createdDatetime: LocalDateTime) {
    jdbcTemplate.update("update OFFENDER_VISITS set CREATE_DATETIME = ? where OFFENDER_VISIT_ID = ?", createdDatetime, visit.id)
  }

  fun lookupVisitType(code: String): VisitType = visitTypeRepository.findByIdOrNull(Pk(VisitType.VISIT_TYPE, code))!!
  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
  fun lookupVisitStatus(code: String): VisitStatus = visitStatusRepository.findByIdOrNull(Pk(VisitStatus.VISIT_STATUS, code))!!
  fun lookupSearchLevelRepository(code: String?): SearchLevel? = code?.let { searchLevelRepository.findByIdOrNull(Pk(SearchLevel.SEARCH_LEVEL, it)) }
  fun lookupAgencyInternalLocationByDescription(description: String): AgencyInternalLocation? = agencyInternalLocationRepository.findOneByDescription(description).getOrNull()
}

@Component
class VisitBuilderFactory(
  private val repository: VisitBuilderRepository,
  private val visitVisitorBuilderFactory: VisitVisitorBuilderFactory,
  private val visitOutcomeBuilderFactory: VisitOutcomeBuilderFactory,
) {
  fun builder() = VisitBuilderRepositoryBuilder(repository, visitVisitorBuilderFactory, visitOutcomeBuilderFactory)
}

class VisitBuilderRepositoryBuilder(
  private val repository: VisitBuilderRepository,
  private val visitVisitorBuilderFactory: VisitVisitorBuilderFactory,
  private val visitOutcomeBuilderFactory: VisitOutcomeBuilderFactory,
) : VisitDsl {

  private lateinit var visit: Visit

  fun build(
    offenderBooking: OffenderBooking,
    visitTypeCode: String,
    visitStatusCode: String,
    startDateTimeString: String,
    endDateTimeString: String,
    agyLocId: String,
    agencyInternalLocationDescription: String?,
    comment: String? = null,
    visitorConcern: String? = null,
    overrideBanStaff: Staff? = null,
    prisonerSearchTypeCode: String? = null,
    visitSlot: AgencyVisitSlot?,
    visitOrder: VisitOrder? = null,
    createdDatetime: LocalDateTime? = null,
  ): Visit = Visit(
    offenderBooking = offenderBooking,
    startDateTime = LocalDateTime.parse(startDateTimeString),
    endDateTime = LocalDateTime.parse(endDateTimeString),
    visitDate = LocalDateTime.parse(startDateTimeString).toLocalDate(),
    visitType = repository.lookupVisitType(visitTypeCode),
    visitStatus = repository.lookupVisitStatus(visitStatusCode),
    location = repository.lookupAgency(agyLocId),
    agencyInternalLocation = agencyInternalLocationDescription?.run {
      repository.lookupAgencyInternalLocationByDescription(
        this,
      )
    },
    agencyVisitSlot = visitSlot,
    visitorConcernText = visitorConcern,
    commentText = comment,
    overrideBanStaff = overrideBanStaff,
    searchLevel = repository.lookupSearchLevelRepository(prisonerSearchTypeCode),
    visitOrder = visitOrder,
  )
    .let { repository.save(it) }
    .also {
      if (createdDatetime != null) {
        repository.updateCreateDatetime(it, createdDatetime)
      }
    }
    .also { visit = it }

  override fun visitor(
    person: Person,
    groupLeader: Boolean,
    assistedVisit: Boolean,
    outcomeReasonCode: String?,
    eventOutcomeCode: String,
    eventStatusCode: String,
    comment: String?,
    dsl: VisitVisitorDsl.() -> Unit,
  ): VisitVisitor = visitVisitorBuilderFactory.builder().let { builder ->
    builder.build(
      visit = visit,
      person = person,
      groupLeader = groupLeader,
      assistedVisit = assistedVisit,
      outcomeReasonCode = outcomeReasonCode,
      eventOutcomeCode = eventOutcomeCode,
      eventStatusCode = eventStatusCode,
      comment = comment,
    )
      .also { visit.visitors += it }
      .also { builder.apply(dsl) }
  }

  override fun visitOutcome(
    outcomeReasonCode: String,
    eventOutcomeCode: String,
    eventStatusCode: String,
    dsl: VisitOutcomeDsl.() -> Unit,
  ): VisitVisitor = visitOutcomeBuilderFactory.builder().let { builder ->
    builder.build(
      visit = visit,
      outcomeReasonCode = outcomeReasonCode,
      eventOutcomeCode = eventOutcomeCode,
      eventStatusCode = eventStatusCode,
    )
      .also { visit.visitors += it }
      .also { builder.apply(dsl) }
  }
}
