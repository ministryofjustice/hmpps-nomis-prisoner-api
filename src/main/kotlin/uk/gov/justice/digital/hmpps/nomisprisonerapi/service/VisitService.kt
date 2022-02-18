package uk.gov.justice.digital.hmpps.nomisprisonerapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CancelVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.VisitIdResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.VisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.filter.VisitFilter
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitOrderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitVisitorRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.VisitSpecification
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
  private val offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val visitTypeRepository: ReferenceCodeRepository<VisitType>,
  private val visitOrderTypeRepository: ReferenceCodeRepository<VisitOrderType>,
  private val visitStatusRepository: ReferenceCodeRepository<VisitStatus>,
  private val visitOutcomeRepository: ReferenceCodeRepository<VisitOutcomeReason>,
  private val eventOutcomeRepository: ReferenceCodeRepository<EventOutcome>,
  private val visitOrderAdjustmentReasonRepository: ReferenceCodeRepository<VisitOrderAdjustmentReason>,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val telemetryClient: TelemetryClient,
  private val personRepository: PersonRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createVisit(offenderNo: String, visitDto: CreateVisitRequest): CreateVisitResponse {

    val offenderBooking = offenderBookingRepository.findByOffenderNomsIdAndActive(offenderNo, true)
      .orElseThrow(NotFoundException(offenderNo))

    val mappedVisit = mapVisitModel(visitDto, offenderBooking)

    createBalance(mappedVisit, visitDto, offenderBooking)

    addVisitors(mappedVisit, offenderBooking, visitDto)

    val visit = visitRepository.save(mappedVisit)

    telemetryClient.trackEvent(
      "visit-created",
      mapOf(
        "nomisVisitId" to visit.id.toString(),
        "offenderNo" to offenderNo,
        "prisonId" to visitDto.prisonId,
      ),
      null
    )
    log.debug("Visit created with Nomis visit id = ${visit.id}")

    return CreateVisitResponse(visit.id)
  }

  fun cancelVisit(offenderNo: String, visitId: Long, visitDto: CancelVisitRequest) {
    val today = LocalDate.now()

    val visit = visitRepository.findById(visitId)
      .orElseThrow(NotFoundException("Nomis visit id $visitId not found"))

    val visitOutcome = visitOutcomeRepository.findById(VisitOutcomeReason.pk(visitDto.outcome))
      .orElseThrow(BadDataException("Invalid cancellation reason: ${visitDto.outcome}"))

    val visitOrder = visit.visitOrder
    if (visit.visitStatus.code == "CANC") {
      val message =
        "Visit already cancelled, with " + if (visitOrder == null) "no outcome" else "outcome " + visitOrder.outcomeReason?.code
      log.error("$message for Nomis visit id = ${visit.id}")
      throw ConflictException(message)
    } else if (visit.visitStatus.code != "SCH") {
      val message = "Visit status is not scheduled but is ${visit.visitStatus.code}"
      log.error("$message for Nomis visit id = ${visit.id}")
      throw ConflictException(message)
    }
    if (offenderNo != visit.offenderBooking.offender.nomsId) {
      val message =
        "Visit's offenderNo = ${visit.offenderBooking.offender.nomsId} does not match argument = $offenderNo"
      log.error("$message for Nomis visit id = ${visit.id}")
      throw BadDataException(message)
    }

    val cancelledVisitStatus = visitStatusRepository.findById(VisitStatus.CANCELLED).orElseThrow()
    val absenceEventOutcome = eventOutcomeRepository.findById(EventOutcome.ABS).orElseThrow()
    val cancelledEventStatus = eventStatusRepository.findById(EventStatus.CANCELLED).orElseThrow()

    visit.visitStatus = cancelledVisitStatus

    visit.visitors.forEach {
      it.eventOutcome = absenceEventOutcome
      it.eventStatus = cancelledEventStatus
      it.outcomeReason = visitOutcome
    }

    if (visitOrder != null) {
      visitOrder.status = cancelledVisitStatus
      visitOrder.outcomeReason = visitOutcome
      visitOrder.expiryDate = today

      cancelBalance(visitOrder, visit.offenderBooking, today)
    }

    telemetryClient.trackEvent(
      "visit-cancelled",
      mapOf(
        "nomisVisitId" to visit.id.toString(),
        "offenderNo" to offenderNo,
        "prisonId" to visit.location.id,
      ),
      null
    )
    log.debug("Visit with Nomis visit id = ${visit.id} cancelled")
  }

  private fun createBalance(
    visit: Visit,
    visitDto: CreateVisitRequest,
    offenderBooking: OffenderBooking,
  ) {
    val offenderVisitBalance = offenderBooking.visitBalance
    if (offenderVisitBalance != null) {

      if (offenderVisitBalance.remainingPrivilegedVisitOrders!! > 0) {
        val adjustReasonCode =
          visitOrderAdjustmentReasonRepository.findById(VisitOrderAdjustmentReason.PVO_ISSUE).orElseThrow()

        offenderVisitBalanceAdjustmentRepository.save(
          OffenderVisitBalanceAdjustment(
            offenderBooking = offenderBooking,
            adjustDate = visitDto.issueDate,
            adjustReasonCode = adjustReasonCode,
            remainingPrivilegedVisitOrders = -1,
            previousRemainingPrivilegedVisitOrders = offenderVisitBalance.remainingPrivilegedVisitOrders,
            commentText = "Created by VSIP",
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
      } else {
        val adjustReasonCode =
          visitOrderAdjustmentReasonRepository.findById(VisitOrderAdjustmentReason.VO_ISSUE).orElseThrow()

        offenderVisitBalanceAdjustmentRepository.save(
          OffenderVisitBalanceAdjustment(
            offenderBooking = offenderBooking,
            adjustDate = visitDto.issueDate,
            adjustReasonCode = adjustReasonCode,
            remainingVisitOrders = -1,
            previousRemainingVisitOrders = offenderVisitBalance.remainingVisitOrders,
            commentText = "Created by VSIP",
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
    visitOrder: VisitOrder,
    offenderBooking: OffenderBooking,
    today: LocalDate,
  ) {
    val offenderVisitBalance = offenderBooking.visitBalance
    if (offenderVisitBalance != null) {

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
  }

  private fun mapVisitModel(visitDto: CreateVisitRequest, offenderBooking: OffenderBooking): Visit {

    val visitType = visitTypeRepository.findById(VisitType.pk(visitDto.visitType))
      .orElseThrow(BadDataException("Invalid visit type: ${visitDto.visitType}"))

    val location = agencyLocationRepository.findById(visitDto.prisonId)
      .orElseThrow(BadDataException("Prison with id=${visitDto.prisonId} does not exist"))

    return Visit(
      offenderBooking = offenderBooking,
      visitDate = LocalDate.from(visitDto.startDateTime),
      startDateTime = visitDto.startDateTime,
      endDateTime = LocalDateTime.of(LocalDate.from(visitDto.startDateTime), visitDto.endTime),
      visitType = visitType,
      visitStatus = visitStatusRepository.findById(VisitStatus.pk("SCH")).orElseThrow(),
      location = location,
    )
  }

  private fun addVisitors(
    visit: Visit,
    offenderBooking: OffenderBooking,
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
      val person = personRepository.findById(it).orElseThrow(BadDataException("Person with id=$it does not exist"))
      visit.visitors.add(
        VisitVisitor(visit = visit, person = person, eventStatus = scheduledEventStatus)
      )
    }
  }

  private fun getNextEvent(): Long {
    return visitVisitorRepository.getEventId()
  }

  fun getVisit(visitId: Long): VisitResponse {
    return visitRepository.findByIdOrNull(visitId)?.run {
      return VisitResponse(this)
    } ?: throw NotFoundException("visit id $visitId")
  }

  fun findVisitIdsByFilter(pageRequest: Pageable, visitFilter: VisitFilter): Page<VisitIdResponse> {
    log.info("Visit Id filter request : $visitFilter with page request $pageRequest")
    return visitRepository.findAll(VisitSpecification(visitFilter), pageRequest).map { VisitIdResponse(it.id) }
  }
}

class NotFoundException(message: String?) : RuntimeException(message), Supplier<NotFoundException> {
  override fun get(): NotFoundException {
    return NotFoundException(message)
  }
}

class BadDataException(message: String?) : RuntimeException(message), Supplier<BadDataException> {
  override fun get(): BadDataException {
    return BadDataException(message)
  }
}

class ConflictException(message: String?) : RuntimeException(message), Supplier<ConflictException> {
  override fun get(): ConflictException {
    return ConflictException(message)
  }
}
