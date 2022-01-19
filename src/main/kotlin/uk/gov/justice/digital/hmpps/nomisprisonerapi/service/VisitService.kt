package uk.gov.justice.digital.hmpps.nomisprisonerapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.AmendVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CancelVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus.Companion.SCHEDULED_APPROVED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitVisitorRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.function.Supplier

@Service
@Transactional
class VisitService(
  private val visitRepository: VisitRepository,
  private val visitVisitorRepository: VisitVisitorRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderVisitBalanceRepository: OffenderVisitBalanceRepository,
  private val offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val visitTypeRepository: ReferenceCodeRepository<VisitType>,
  private val visitStatusRepository: ReferenceCodeRepository<VisitStatus>,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val telemetryClient: TelemetryClient,
  private val personRepository: PersonRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createVisit(offenderNo: String, visitDto: CreateVisitRequest): CreateVisitResponse {

    val offenderBooking = offenderBookingRepository.findByOffenderNomsIdAndActive(offenderNo, true)
      .orElseThrow(PrisonerNotFoundException(offenderNo))

    createBalance(visitDto, offenderBooking)

    val mappedVisit = mapVisitModel(visitDto, offenderBooking)

    addVisitors(mappedVisit, offenderBooking, visitDto)

    val visit = visitRepository.save(mappedVisit)

    telemetryClient.trackEvent("visit-created", mapOf("visitId" to visit.id.toString()), null)

    return CreateVisitResponse(visit.id)
  }

  fun amendVisit(offenderNo: String, visitId: Long, visitDto: AmendVisitRequest) {
  }

  fun cancelVisit(offenderNo: String, visitId: Long, visitDto: CancelVisitRequest) {
  }

  private fun createBalance(
    visitDto: CreateVisitRequest,
    offenderBooking: OffenderBooking,
  ) {
    val offenderVisitBalance = offenderVisitBalanceRepository.findById(offenderBooking.bookingId).orElseThrow()

    if (visitDto.decrementBalances) {

      if (visitDto.privileged) {

        if (offenderVisitBalance.remainingPrivilegedVisitOrders!! > 0) {
          offenderVisitBalanceAdjustmentRepository.save(
            OffenderVisitBalanceAdjustment(
              offenderBooking = offenderBooking,
              adjustDate = visitDto.issueDate,
              adjustReasonCode = VisitOrderAdjustmentReason.PVO_ISSUE,
              remainingPrivilegedVisitOrders = -1,
              previousRemainingPrivilegedVisitOrders = offenderVisitBalance.remainingPrivilegedVisitOrders,
              commentText = "Created by PVB3 for an on-line visit booking",
              //  authorisedStaffId = visitDto.staffId,
              //  endorsedStaffId = visitDto.staffId,
            )
          )
        }
      } else {
        if (offenderVisitBalance.remainingVisitOrders!! > 0) {
          offenderVisitBalanceAdjustmentRepository.save(
            OffenderVisitBalanceAdjustment(
              offenderBooking = offenderBooking,
              adjustDate = visitDto.issueDate,
              adjustReasonCode = VisitOrderAdjustmentReason.VISIT_ORDER_ISSUE,
              remainingVisitOrders = -1,
              previousRemainingVisitOrders = offenderVisitBalance.remainingVisitOrders,
              commentText = "Created by PVB3 for an on-line visit booking",
              //  authorisedStaffId = visitDto.staffId,
              //  endorsedStaffId = visitDto.staffId,
              // TODO: If necessary, could follow stored proc logic and use staffId from:
              //   select staff_id from staff_user_accounts where username = 'OMS_OWNER';
            )
          )
        }
      }
    }
  }

  private fun mapVisitModel(visitDto: CreateVisitRequest, offenderBooking: OffenderBooking): Visit {

    val visitType = visitTypeRepository.findById(VisitType.pk(visitDto.visitType)).orElseThrow()

    val location = agencyLocationRepository.findById(visitDto.prisonId)
      .orElseThrow(DataNotFoundException("Prison with id=${visitDto.prisonId} does not exist"))
    val agencyInternalLocations =
      agencyInternalLocationRepository.findByLocationCodeAndAgencyId(visitDto.visitRoomId, visitDto.prisonId)
    if (agencyInternalLocations.isEmpty()) {
      throw (DataNotFoundException("Room location with code=${visitDto.visitRoomId} does not exist in prison ${visitDto.prisonId}"))
    } else if (agencyInternalLocations.size > 1) {
      throw (DataNotFoundException("There is more than one room with code=${visitDto.visitRoomId} at prison ${visitDto.prisonId}"))
    }
    // TODO is more validation needed on prison or room (e.g. it is a visit room type)?

    return Visit(
      offenderBooking = offenderBooking,
      visitDate = LocalDate.from(visitDto.startDateTime),
      startTime = visitDto.startDateTime,
      endTime = LocalDateTime.of(LocalDate.from(visitDto.startDateTime), visitDto.endTime),
      visitType = visitType,
      visitStatus = visitStatusRepository.findById(VisitStatus.pk("SCH")).orElseThrow(),
      location = location,
      agencyInternalLocation = agencyInternalLocations[0],
      // TODO not yet sure if anything else is needed

      // searchLevel = searchRepository.findById(SearchLevel.pk("FULL")).orElseThrow(),
      // agencyInternalLocation = agencyInternalRepository.findById(-3L).orElseThrow(),
      // commentText = "comment text",
      // visitorConcernText = "visitor concerns",
    )
  }

  private fun addVisitors(
    visit: Visit,
    offenderBooking: OffenderBooking?,
    visitDto: CreateVisitRequest
  ) {
    val scheduledEventStatus = eventStatusRepository.findById(SCHEDULED_APPROVED).orElseThrow()

    //  Add dummy visitor row for the offender_booking as is required by the P-Nomis view

    visit.visitors.add(
      VisitVisitor(
        visit = visit,
        offenderBooking = offenderBooking,
        eventStatus = scheduledEventStatus,
        eventId = getNextEvent()
      )
    )

    visitDto.visitorPersonIds.forEach {
      val person = personRepository.findById(it).orElseThrow(DataNotFoundException("Person with id=$it does not exist"))
      visit.visitors.add(
        VisitVisitor(visit = visit, person = person, eventStatus = scheduledEventStatus, eventId = getNextEvent())
      )
    }
  }

  private fun getNextEvent(): Long {
    // TODO Reviewing with Paul M as to whether this is a wise course to take!
    // The stored proc uses event_id.nextval
    return visitVisitorRepository.getEventId()
  }
}

class PrisonerNotFoundException(message: String?) : RuntimeException(message), Supplier<PrisonerNotFoundException> {
  override fun get(): PrisonerNotFoundException {
    return PrisonerNotFoundException(message)
  }
}

class DataNotFoundException(message: String?) : RuntimeException(message), Supplier<DataNotFoundException> {
  override fun get(): DataNotFoundException {
    return DataNotFoundException(message)
  }
}
