package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.usernamePreferringGeneralAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SearchLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOutcomeReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType.Companion.OFFICIAL
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitSlotRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderContactPersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitVisitorRepository
import java.time.LocalDate

@Service
@Transactional
class OfficialVisitsService(
  private val visitRepository: VisitRepository,
  private val visitVisitorRepository: VisitVisitorRepository,
  private val offenderContactPersonRepository: OffenderContactPersonRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyVisitSlotRepository: AgencyVisitSlotRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val visitStatusRepository: ReferenceCodeRepository<VisitStatus>,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val visitTypeRepository: ReferenceCodeRepository<VisitType>,
  private val searchLevelRepository: ReferenceCodeRepository<SearchLevel>,
  private val eventOutcomeRepository: ReferenceCodeRepository<EventOutcome>,
  private val visitOutcomeReasonRepository: ReferenceCodeRepository<VisitOutcomeReason>,
  private val staffUserAccountRepository: StaffUserAccountRepository,
  private val personRepository: PersonRepository,
) {
  fun getVisitIds(
    pageRequest: Pageable,
    prisonIds: List<String>,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): Page<VisitIdResponse> = if (prisonIds.isEmpty()) {
    if (fromDate == null && toDate == null) {
      visitRepository.findAllOfficialVisitsIds(pageRequest)
    } else {
      visitRepository.findAllOfficialVisitsIdsWithDateFilter(
        toDate = toDate?.atStartOfDay()?.plusDays(1),
        fromDate = fromDate?.atStartOfDay(),
        pageable = pageRequest,
      )
    }
  } else {
    visitRepository.findAllOfficialVisitsIdsWithDateAndPrisonFilter(
      toDate = toDate?.atStartOfDay()?.plusDays(1),
      fromDate = fromDate?.atStartOfDay(),
      prisonIds = prisonIds,
      pageable = pageRequest,
    )
  }.map {
    VisitIdResponse(
      visitId = it.id,
    )
  }
  fun getVisitIds(
    visitId: Long,
    pageSize: Int,
    prisonIds: List<String>,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): VisitIdsPage = if (prisonIds.isEmpty()) {
    if (fromDate == null && toDate == null) {
      visitRepository.findAllOfficialVisitsIds(
        visitId = visitId,
        pageSize = pageSize,
      )
    } else {
      visitRepository.findAllOfficialVisitsIdsWithDateFilter(
        toDate = toDate?.atStartOfDay()?.plusDays(1),
        fromDate = fromDate?.atStartOfDay(),
        visitId = visitId,
        pageSize = pageSize,
      )
    }
  } else {
    visitRepository.findAllOfficialVisitsIdsWithDateAndPrisonFilter(
      toDate = toDate?.atStartOfDay()?.plusDays(1),
      fromDate = fromDate?.atStartOfDay(),
      prisonIds = prisonIds,
      visitId = visitId,
      pageSize = pageSize,
    )
  }.map {
    VisitIdResponse(
      visitId = it.id,
    )
  }.let { VisitIdsPage(it) }

  @Audit(auditModule = "DPS_SYNCHRONISATION_OFFICIAL_VISITS")
  fun createVisitForPrisoner(offenderNo: String, request: CreateOfficialVisitRequest): OfficialVisitResponse {
    val offenderBooking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Prisoner $offenderNo not found with a booking")
    val location = lookupAgency(request.prisonId)
    val agencyInternalLocation = lookupInternalLocation(request.internalLocationId)
    val visitSlot = lookupVisitSlot(request.visitSlotId)
    val visitStatus = lookupVisitStatus(request.visitStatusCode)
    val visitType = lookupOfficialVisitType()
    val searchLevel: SearchLevel? = lookupSearchLevel(request.prisonerSearchTypeCode)

    return visitRepository.saveAndFlush(
      Visit(
        offenderBooking = offenderBooking,
        commentText = request.commentText,
        visitorConcernText = request.visitorConcernText,
        visitDate = request.startDateTime.toLocalDate(),
        startDateTime = request.startDateTime,
        endDateTime = request.endDateTime,
        visitType = visitType,
        visitStatus = visitStatus,
        searchLevel = searchLevel,
        location = location,
        agencyInternalLocation = agencyInternalLocation,
        agencyVisitSlot = visitSlot,
        overrideBanStaff = request.overrideBanStaffUsername?.let { staffOf(it) },
      ),
    ).also { visit ->
      visit.visitors += VisitVisitor(
        offenderBooking = offenderBooking,
        visit = visit,
        person = null,
        groupLeader = false,
        assistedVisit = false,
        commentText = null,
        eventStatus = lookupEventStatus(request.overallVisitStatus.name),
        eventId = nextEventId(),
        outcomeReasonCode = null,
        eventOutcome = lookupAttendance(request.prisonerAttendanceCode),
      )
    }.toOfficialVisitResponse()
  }

  @Audit(auditModule = "DPS_SYNCHRONISATION_OFFICIAL_VISITS")
  fun updateVisit(visitId: Long, request: UpdateOfficialVisitRequest) {
    val visit = visitRepository.findByIdOrNullForUpdate(visitId) ?: throw NotFoundException("Visit with id $visitId not found")

    with(visit) {
      commentText = request.commentText
      visitorConcernText = request.visitorConcernText
      agencyVisitSlot = lookupVisitSlot(request.visitSlotId)
      visitDate = request.startDateTime.toLocalDate()
      startDateTime = request.startDateTime
      endDateTime = request.endDateTime
      visitStatus = lookupVisitStatus(request.visitStatusCode)
      agencyInternalLocation = lookupInternalLocation(request.internalLocationId)
      searchLevel = lookupSearchLevel(request.prisonerSearchTypeCode)
      overrideBanStaff = request.overrideBanStaffUsername?.let { staffOf(it) }
      with(outcomeVisitor()!!) {
        eventStatus = lookupEventStatus(request.overallVisitStatus.name)
        outcomeReasonCode = lookupOutcomeReason(request.visitOutcomeCode)?.code
        eventOutcome = lookupAttendance(request.prisonerAttendanceCode)
      }
    }
  }

  @Audit(auditModule = "DPS_SYNCHRONISATION_OFFICIAL_VISITS")
  fun createVisitorForVisit(
    visitId: Long,
    request: CreateOfficialVisitorRequest,
  ): OfficialVisitResponse.OfficialVisitor {
    val visit = visitRepository.findByIdOrNullForUpdate(visitId) ?: throw NotFoundException("Visit with id $visitId not found")
    val person = personRepository.findByIdOrNull(request.personId) ?: throw BadDataException("Person with id ${request.personId} not found")
    return visitVisitorRepository.saveAndFlush(
      VisitVisitor(
        offenderBooking = null,
        visit = visit,
        person = person,
        groupLeader = request.leadVisitor ?: false,
        assistedVisit = request.assistedVisit ?: false,
        commentText = request.commentText,
        eventStatus = lookupEventStatus(request.overallVisitStatus.name),
        eventId = nextEventId(),
        outcomeReasonCode = null,
        eventOutcome = lookupAttendance(request.visitorAttendanceOutcomeCode),
      ),
    ).toOfficialVisitor(visit.offenderBooking)
  }

  @Audit(auditModule = "DPS_SYNCHRONISATION_OFFICIAL_VISITS")
  fun updateVisitorForVisit(
    visitId: Long,
    visitorId: Long,
    request: UpdateOfficialVisitorRequest,
  ) {
    visitVisitorRepository.findByIdOrNullForUpdate(visitorId)?.also { visitor ->
      val visit = visitRepository.findByIdOrNullForUpdate(visitId) ?: throw NotFoundException("Visit with id $visitId not found")
      if (visitor.visit != visit) {
        throw BadDataException("Visitor with id $visitorId does not belong to visit with id $visitId")
      }
      with(visitor) {
        groupLeader = request.leadVisitor ?: false
        assistedVisit = request.assistedVisit ?: false
        commentText = request.commentText
        eventStatus = lookupEventStatus(request.overallVisitStatus.name)
        eventOutcome = lookupAttendance(request.visitorAttendanceOutcomeCode)
        outcomeReasonCode = lookupOutcomeReason(request.visitOutcomeCode)?.code
      }
    } ?: throw NotFoundException("Visitor with id $visitorId is not found")
  }

  @Audit(auditModule = "DPS_SYNCHRONISATION_OFFICIAL_VISITS")
  fun deleteVisitorForVisit(visitId: Long, visitorId: Long) {
    visitRepository.findByIdOrNullForUpdate(visitId) ?: throw NotFoundException("Visit with id $visitId not found")
    visitVisitorRepository.findByIdOrNullForUpdate(visitorId)?.also { visitor ->
      if (visitor.visit.id != visitId) {
        throw BadDataException("Visitor with id $visitorId does not belong to visit with id $visitId")
      }
      visitVisitorRepository.delete(visitor)
    }
  }

  fun getVisit(visitId: Long): OfficialVisitResponse {
    val officialVisit = (visitRepository.findByIdOrNull(visitId) ?: throw NotFoundException("Visit with id $visitId not found")).takeIf { it.visitType.isOfficial() } ?: throw BadDataException("Visit with id $visitId is not an official visit")

    return officialVisit.toOfficialVisitResponse()
  }

  fun getVisitsForPrisoner(offenderNo: String, fromDate: LocalDate?, toDate: LocalDate?): List<OfficialVisitResponse> = visitRepository.findAllOfficialVisitsByOffenderNoWithDateFilter(
    offenderNo = offenderNo,
    fromDate = fromDate,
    toDate = toDate,
  ).map { it.toOfficialVisitResponse() }

  fun Visit.toOfficialVisitResponse() = OfficialVisitResponse(
    visitId = id,
    // only one social visit has no slot
    visitSlotId = agencyVisitSlot!!.id,
    prisonId = location.id,
    offenderNo = offenderBooking.offender.nomsId,
    bookingId = offenderBooking.bookingId,
    currentTerm = offenderBooking.bookingSequence == 1,
    startDateTime = startDateTime,
    endDateTime = endDateTime,
    // only one social visit has no location
    internalLocationId = agencyInternalLocation!!.locationId,
    visitStatus = visitStatus.toCodeDescription(),
    visitOutcome = outcomeVisitor()?.eventStatus?.toCodeDescription(),
    // A couple of outcome reason codes have no NOMIS reference data - so fall back on code ony for a NotFound scenario
    cancellationReason = outcomeVisitor()?.outcomeReason?.toCodeDescription() ?: outcomeVisitor()?.outcomeReasonCode?.let { CodeDescription(it, it) },
    prisonerAttendanceOutcome = outcomeVisitor()?.eventOutcome?.toCodeDescription(),
    prisonerSearchType = searchLevel?.toCodeDescription(),
    visitorConcernText = visitorConcernText,
    commentText = commentText,
    overrideBanStaffUsername = overrideBanStaff?.usernamePreferringGeneralAccount(),
    visitOrder = visitOrder?.let { OfficialVisitResponse.VisitOrder(number = it.visitOrderNumber) },
    visitors = visitors.filter { it.person != null }.map { it.toOfficialVisitor(offenderBooking) },
    audit = toAudit(),
  )

  private fun VisitVisitor.toOfficialVisitor(offenderBooking: OffenderBooking): OfficialVisitResponse.OfficialVisitor = OfficialVisitResponse.OfficialVisitor(
    id = id,
    personId = person!!.id,
    firstName = person!!.firstName,
    lastName = person!!.lastName,
    dateOfBirth = person!!.birthDate,
    leadVisitor = groupLeader,
    assistedVisit = assistedVisit,
    visitorAttendanceOutcome = eventOutcome?.toCodeDescription(),
    cancellationReason = outcomeReason?.toCodeDescription()
      ?: outcomeReasonCode?.let { CodeDescription(it, it) },
    eventStatus = eventStatus?.toCodeDescription(),
    commentText = commentText,
    relationships = offenderContactPersonRepository.findByPersonAndOffenderBooking(person!!, offenderBooking)
      .sortedWith(latestOfficialContactFirst()).map {
        OfficialVisitResponse.OfficialVisitor.ContactRelationship(
          relationshipType = it.relationshipType.toCodeDescription(),
          contactType = it.contactType.toCodeDescription(),
        )
      },
    audit = toAudit(),
  )

  private fun lookupAgency(prisonId: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(prisonId) ?: throw BadDataException("Prison $prisonId does not exist")
  private fun lookupInternalLocation(internalLocationId: Long): AgencyInternalLocation = agencyInternalLocationRepository.findByIdOrNull(internalLocationId) ?: throw BadDataException("Internal location $internalLocationId does not exist")
  private fun lookupVisitSlot(visitSlotId: Long): AgencyVisitSlot = agencyVisitSlotRepository.findByIdOrNull(visitSlotId) ?: throw BadDataException("Visit slot $visitSlotId does not exist")
  private fun lookupVisitStatus(code: String): VisitStatus = visitStatusRepository.findByIdOrNull(VisitStatus.pk(code)) ?: throw BadDataException("Visit status code $code does not exist")
  private fun lookupEventStatus(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code)) ?: throw BadDataException("Event status code $code does not exist")
  private fun lookupOutcomeReason(code: String?): VisitOutcomeReason? = code?.let { visitOutcomeReasonRepository.findByIdOrNull(VisitOutcomeReason.pk(code)) ?: throw BadDataException("Visit outcome reason code $code does not exist") }
  private fun lookupOfficialVisitType(): VisitType = visitTypeRepository.findByIdOrNull(VisitType.pk(OFFICIAL)) ?: throw BadDataException("Visit status code $OFFICIAL does not exist")
  private fun staffOf(username: String): Staff = staffUserAccountRepository.findByUsername(username)?.staff ?: throw BadDataException("Staff with username=$username does not exist")
  private fun lookupSearchLevel(code: String?) = code?.let { searchLevelRepository.findByIdOrNull(SearchLevel.pk(code)) ?: throw BadDataException("Search Level code $code does not exist") }
  private fun lookupAttendance(code: String?) = code?.let { eventOutcomeRepository.findByIdOrNull(EventOutcome.pk(code)) ?: throw BadDataException("Event Outcome code $code does not exist") }
  private fun nextEventId(): Long = visitVisitorRepository.getEventId()
}

private fun latestOfficialContactFirst() = compareByDescending<OffenderContactPerson> { it.relationshipType.code }.thenByDescending { it.createDatetime }
