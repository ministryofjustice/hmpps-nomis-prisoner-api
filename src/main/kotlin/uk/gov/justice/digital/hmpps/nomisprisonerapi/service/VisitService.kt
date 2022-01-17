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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceRepository
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
  private val agencyRepository: AgencyLocationRepository,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createVisit(visitDto: CreateVisitRequest): CreateVisitResponse {

    val offenderBooking = offenderBookingRepository.findByOffenderNomsIdAndActive(visitDto.offenderNo, true)
      .orElseThrow(PrisonerNotFoundException(visitDto.offenderNo))

    createBalance(visitDto, offenderBooking)

    val visit = visitRepository.save(mapVisitModel(visitDto, offenderBooking))

    addVisitors(visit, offenderBooking, visitDto)

    telemetryClient.trackEvent("visit-created", mapOf("visitId" to visit.id!!.toString()), null)

    return CreateVisitResponse(visit.id)
  }

  fun amendVisit(visitId: Long, visitDto: AmendVisitRequest) {
  }

  fun cancelVisit(visitId: Long, visitDto: CancelVisitRequest) {
  }

  private fun createBalance(
    visitDto: CreateVisitRequest,
    offenderBooking: OffenderBooking,
  ) {
    val offenderVisitBalance = offenderVisitBalanceRepository.findById(offenderBooking.bookingId!!).orElseThrow()

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

    return Visit(
      offenderBooking = offenderBooking,
      visitDate = LocalDate.from(visitDto.startTime),
      startTime = visitDto.startTime,
      endTime = LocalDateTime.of(LocalDate.from(visitDto.startTime), visitDto.endTime),
      visitType = visitType,
      visitStatus = visitStatusRepository.findById(VisitStatus.pk("SCH")).orElseThrow(),
      location = agencyRepository.findById(visitDto.prisonId).orElseThrow(),
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

    visitVisitorRepository.save(
      VisitVisitor(
        visitId = visit.id!!,
        offenderBooking = offenderBooking,
        eventStatus = scheduledEventStatus,
      )
    )

    visitDto.visitorPersonIds.forEach {
      visitVisitorRepository.save(
        VisitVisitor(
          visitId = visit.id,
          personId = it,
          eventStatus = scheduledEventStatus,
        )
      )
    }
  }
}

class PrisonerNotFoundException(message: String?) : RuntimeException(message), Supplier<PrisonerNotFoundException> {
  override fun get(): PrisonerNotFoundException {
    return PrisonerNotFoundException(message)
  }
}
