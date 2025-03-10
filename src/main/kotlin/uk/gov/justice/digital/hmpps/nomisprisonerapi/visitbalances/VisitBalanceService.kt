package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances

import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceRepository
import java.time.LocalDate

@Service
@Transactional
class VisitBalanceService(
  val visitBalanceRepository: OffenderVisitBalanceRepository,
  val offenderBookingRepository: OffenderBookingRepository,
  val offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository,
) {

  fun findAllIds(prisonId: String?, pageRequest: Pageable): Page<VisitBalanceIdResponse> = visitBalanceRepository.findForLatestBooking(prisonId, pageRequest)
    .map { VisitBalanceIdResponse(it.offenderBookingId) }

  fun getVisitOrderBalance(offenderNo: String): PrisonerVisitOrderBalanceResponse = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
    ?.let { latestBooking ->
      val fromDate = LocalDate.now().minusMonths(1)

      PrisonerVisitOrderBalanceResponse(
        remainingVisitOrders = latestBooking.visitBalance?.remainingVisitOrders ?: 0,
        remainingPrivilegedVisitOrders = latestBooking.visitBalance?.remainingPrivilegedVisitOrders ?: 0,
        visitOrderBalanceAdjustments = latestBooking.visitBalanceAdjustments.filter {
          it.adjustDate.let { adjDate -> !adjDate.isBefore(fromDate) }
        }.map { it.toVisitBalanceAdjustmentResponse() },
      )
    } ?: throw NotFoundException("Prisoner with offender no $offenderNo not found with any bookings")

  fun getVisitBalanceAdjustment(visitBalanceAdjustmentId: Long): VisitBalanceAdjustmentResponse = offenderVisitBalanceAdjustmentRepository.findById(visitBalanceAdjustmentId)
    .map { it.toVisitBalanceAdjustmentResponse() }
    .orElseThrow { NotFoundException("Visit balance adjustment with id $visitBalanceAdjustmentId not found") }
}
