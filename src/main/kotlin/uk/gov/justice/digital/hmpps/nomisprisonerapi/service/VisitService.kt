package uk.gov.justice.digital.hmpps.nomisprisonerapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.AmendVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CancelVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventOutcome
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus.Companion.SCHEDULED_APPROVED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOutcomeReason
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitOrderRepository
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
  private val visitOrderRepository: VisitOrderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderVisitBalanceRepository: OffenderVisitBalanceRepository,
  private val offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val visitTypeRepository: ReferenceCodeRepository<VisitType>,
  private val visitOrderTypeRepository: ReferenceCodeRepository<VisitOrderType>,
  private val visitStatusRepository: ReferenceCodeRepository<VisitStatus>,
  private val visitOutcomeRepository: ReferenceCodeRepository<VisitOutcomeReason>,
  private val eventOutcomeRepository: ReferenceCodeRepository<EventOutcome>,
  private val visitOrderAdjustmentReasonRepository: ReferenceCodeRepository<VisitOrderAdjustmentReason>,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val telemetryClient: TelemetryClient,
  private val personRepository: PersonRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    val VSIP_PREFIX = "VSIP_"
  }

  fun createVisit(offenderNo: String, visitDto: CreateVisitRequest): CreateVisitResponse {

    val offenderBooking = offenderBookingRepository.findByOffenderNomsIdAndActive(offenderNo, true)
      .orElseThrow(PrisonerNotFoundException(offenderNo))

    val mappedVisit = mapVisitModel(visitDto, offenderBooking)

    createBalance(mappedVisit, visitDto, offenderBooking)

    addVisitors(mappedVisit, offenderBooking, visitDto)

    val visit = visitRepository.save(mappedVisit)

    telemetryClient.trackEvent("visit-created", mapOf("visitId" to visit.id.toString()), null)

    return CreateVisitResponse(visit.id)
  }

  fun amendVisit(offenderNo: String, visitId: Long, visitDto: AmendVisitRequest) {
  }

  fun cancelVisit(offenderNo: String, visitId: Long, visitDto: CancelVisitRequest) {
    val today = LocalDate.now()
    val visit = visitRepository.findById(visitId).orElseThrow(PrisonerNotFoundException(offenderNo))
    val visitOutcome = visitOutcomeRepository.findById(VisitOutcomeReason.pk(visitDto.outcome)).orElseThrow()

    val cancVisitStatus = visitStatusRepository.findById(VisitStatus.CANCELLED).orElseThrow()
    val absEventOutcome = eventOutcomeRepository.findById(EventOutcome.ABS).orElseThrow()
    val cancEventStatus = eventStatusRepository.findById(EventStatus.CANCELLED).orElseThrow()

    if (visit.visitStatus?.code == "CANC") {
      throw DataNotFoundException("Visit already cancelled") // or 2xx already done?
    } else if (visit.visitStatus?.code != "SCH") {
      throw DataNotFoundException("Visit already completed")
    }

    visit.visitStatus = cancVisitStatus

    visit.visitors.forEach {
      it.eventOutcome = absEventOutcome
      it.eventStatus = cancEventStatus
      it.outcomeReason = visitOutcome
    }

    val visitOrder = visit.visitOrder
    if (visitOrder != null) {
      visitOrder.status = cancVisitStatus
      visitOrder.outcomeReason = visitOutcome
      visitOrder.expiryDate = today

      cancelBalance(visit, today)
    }
  }

  private fun createBalance(
    visit: Visit,
    visitDto: CreateVisitRequest,
    offenderBooking: OffenderBooking,
  ) {
    val offenderVisitBalance = offenderVisitBalanceRepository.findById(offenderBooking.bookingId)

    if (offenderVisitBalance.isPresent) {

      if (offenderVisitBalance.get().remainingPrivilegedVisitOrders!! > 0) {
        val adjustReasonCode =
          visitOrderAdjustmentReasonRepository.findById(VisitOrderAdjustmentReason.PVO_ISSUE).orElseThrow()

        offenderVisitBalanceAdjustmentRepository.save(
          OffenderVisitBalanceAdjustment(
            offenderBooking = offenderBooking,
            adjustDate = visitDto.issueDate,
            adjustReasonCode = adjustReasonCode,
            remainingPrivilegedVisitOrders = -1,
            previousRemainingPrivilegedVisitOrders = offenderVisitBalance.get().remainingPrivilegedVisitOrders,
            commentText = "Created by VSIP for an on-line visit booking",
          )
        )
        visit.visitOrder = VisitOrder(
          offenderBooking = offenderBooking,
          visitOrderNumber = visitOrderRepository.getVisitOrderNumber(),
          visitOrderType = visitOrderTypeRepository.findById(VisitOrderType.pk("PVO")).orElseThrow(),
          status = visitStatusRepository.findById(VisitStatus.pk("SCH")).orElseThrow(),
          issueDate = visitDto.issueDate,
          expiryDate = visitDto.issueDate.plusDays(28),
        )
      } else if (offenderVisitBalance.get().remainingVisitOrders!! > 0) {
        val adjustReasonCode =
          visitOrderAdjustmentReasonRepository.findById(VisitOrderAdjustmentReason.VO_ISSUE).orElseThrow()
        offenderVisitBalanceAdjustmentRepository.save(
          OffenderVisitBalanceAdjustment(
            offenderBooking = offenderBooking,
            adjustDate = visitDto.issueDate,
            adjustReasonCode = adjustReasonCode,
            remainingVisitOrders = -1,
            previousRemainingVisitOrders = offenderVisitBalance.get().remainingVisitOrders,
            commentText = "Created by VSIP for an on-line visit booking",
            //  authorisedStaffId = visitDto.staffId,
            //  endorsedStaffId = visitDto.staffId,
            // TODO: If necessary, could follow stored proc logic and use staffId from:
            //   select staff_id from staff_user_accounts where username = 'OMS_OWNER';
          )
        )
        visit.visitOrder = VisitOrder(
          offenderBooking = offenderBooking,
          visitOrderNumber = visitOrderRepository.getVisitOrderNumber(),
          visitOrderType = visitOrderTypeRepository.findById(VisitOrderType.pk("VO")).orElseThrow(),
          status = visitStatusRepository.findById(VisitStatus.pk("SCH")).orElseThrow(),
          issueDate = visitDto.issueDate,
          expiryDate = visitDto.issueDate.plusDays(28),
        )
      }
    }
  }

  private fun cancelBalance(
    visit: Visit,
    today: LocalDate,
  ) {
    val visitOrder = visit.visitOrder!!
    val offenderBooking = visit.offenderBooking!!
    val offenderVisitBalance = offenderVisitBalanceRepository.findById(offenderBooking.bookingId).orElseThrow()

    offenderVisitBalanceAdjustmentRepository.save(

      if (visitOrder.visitOrderType.isPrivileged())
        OffenderVisitBalanceAdjustment(
          offenderBooking = offenderBooking,
          adjustDate = today,
          commentText = "Booking cancelled by VSIP",
          adjustReasonCode = visitOrderAdjustmentReasonRepository.findById(VisitOrderAdjustmentReason.PVO_CANCEL)
            .orElseThrow(),
          remainingPrivilegedVisitOrders = 1,
          previousRemainingPrivilegedVisitOrders = offenderVisitBalance.remainingPrivilegedVisitOrders,
        )
      else
        OffenderVisitBalanceAdjustment(
          offenderBooking = offenderBooking,
          adjustDate = today,
          commentText = "Booking cancelled by VSIP",
          adjustReasonCode = visitOrderAdjustmentReasonRepository.findById(VisitOrderAdjustmentReason.VO_CANCEL)
            .orElseThrow(),
          remainingVisitOrders = 1,
          previousRemainingVisitOrders = offenderVisitBalance.remainingVisitOrders,
        )
    )
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
      vsipVisitId = VSIP_PREFIX + visitDto.vsipVisitId
      // TODO not yet sure if anything else is needed

      // searchLevel = searchRepository.findById(SearchLevel.pk("FULL")).orElseThrow(),
      // commentText = "booked by VSIP",
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
        VisitVisitor(visit = visit, person = person, eventStatus = scheduledEventStatus)
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
