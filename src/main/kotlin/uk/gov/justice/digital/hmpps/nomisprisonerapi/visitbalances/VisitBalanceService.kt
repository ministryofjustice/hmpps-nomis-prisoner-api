package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances

import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceRepository

@Service
@Transactional
class VisitBalanceService(
  val visitBalanceRepository: OffenderVisitBalanceRepository,
  val offenderBookingRepository: OffenderBookingRepository,
  val offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository,
) {

  fun findAllIds(prisonId: String?, pageRequest: Pageable): Page<VisitBalanceIdResponse> = visitBalanceRepository.findForLatestBooking(prisonId, pageRequest)
    .map { VisitBalanceIdResponse(it.offenderBookingId) }

  fun getVisitBalanceById(visitBalanceId: Long): PrisonerVisitBalanceResponse = offenderBookingRepository.findById(visitBalanceId).map { getVisitBalance(it) }.orElseThrow {
    NotFoundException("Prisoner with booking number $visitBalanceId not found with any bookings")
  }

  fun getVisitBalanceForPrisoner(offenderNo: String): PrisonerVisitBalanceResponse = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)?.let {
    getVisitBalance(it)
  } ?: throw NotFoundException("Prisoner with offender no $offenderNo not found with any bookings")

  private fun getVisitBalance(latestBooking: OffenderBooking): PrisonerVisitBalanceResponse {
    val lastBatchIEPAdjustmentDate = latestBooking.visitBalanceAdjustments
      .filter { it.isIEPAllocation() && it.isCreatedByBatchVOProcess() }.maxByOrNull { it.adjustDate }?.adjustDate

    return PrisonerVisitBalanceResponse(
      prisonNumber = latestBooking.offender.nomsId,
      remainingVisitOrders = latestBooking.visitBalance?.remainingVisitOrders ?: 0,
      remainingPrivilegedVisitOrders = latestBooking.visitBalance?.remainingPrivilegedVisitOrders ?: 0,
      lastIEPAllocationDate = lastBatchIEPAdjustmentDate ?: latestBooking.visitBalanceAdjustments.filter { it.isIEPAllocation() }.maxByOrNull { it.adjustDate }?.adjustDate,
    )
  }

  fun getVisitBalanceAdjustment(visitBalanceAdjustmentId: Long): VisitBalanceAdjustmentResponse = offenderVisitBalanceAdjustmentRepository.findById(visitBalanceAdjustmentId)
    .map { it.toVisitBalanceAdjustmentResponse() }
    .orElseThrow { NotFoundException("Visit balance adjustment with id $visitBalanceAdjustmentId not found") }
}

fun OffenderVisitBalanceAdjustment.isCreatedByBatchVOProcess() = createUsername == "OMS_OWNER"
fun OffenderVisitBalanceAdjustment.isIEPAllocation() = adjustReasonCode.code == IEP_ENTITLEMENT
