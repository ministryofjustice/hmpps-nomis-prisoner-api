package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
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
    dsl: VisitVisitorDsl.() -> Unit = {},
  ): VisitVisitor

  @VisitOutcomeDslMarker
  fun visitOutcome(
    outcomeReason: String,
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
) {
  fun save(visit: Visit): Visit = visitRepository.save(visit)
  fun lookupVisitType(code: String): VisitType = visitTypeRepository.findByIdOrNull(Pk(VisitType.VISIT_TYPE, code))!!
  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
  fun lookupVisitStatus(code: String): VisitStatus = visitStatusRepository.findByIdOrNull(Pk(VisitStatus.VISIT_STATUS, code))!!
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
    visitSlot: AgencyVisitSlot?,
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
  )
    .let { repository.save(it) }
    .also { visit = it }

  override fun visitor(person: Person, groupLeader: Boolean, dsl: VisitVisitorDsl.() -> Unit): VisitVisitor = visitVisitorBuilderFactory.builder().let { builder ->
    builder.build(
      visit = visit,
      person = person,
      groupLeader = groupLeader,
    )
      .also { visit.visitors += it }
      .also { builder.apply(dsl) }
  }

  override fun visitOutcome(outcomeReason: String, dsl: VisitOutcomeDsl.() -> Unit): VisitVisitor = visitOutcomeBuilderFactory.builder().let { builder ->
    builder.build(
      visit = visit,
      outcomeReason = outcomeReason,
    )
      .also { visit.visitors += it }
      .also { builder.apply(dsl) }
  }
}
