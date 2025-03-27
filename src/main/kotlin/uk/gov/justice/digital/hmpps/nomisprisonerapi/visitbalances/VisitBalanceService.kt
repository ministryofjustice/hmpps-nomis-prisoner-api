package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
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
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
  fun findAllIds(prisonId: String?, pageRequest: Pageable): Page<VisitBalanceIdResponse> = visitBalanceRepository.findForLatestBooking(prisonId, pageRequest)
    .map { VisitBalanceIdResponse(it.offenderBookingId) }

  fun getVisitBalanceById(visitBalanceId: Long): VisitBalanceDetailResponse = offenderBookingRepository.findByIdOrNull(visitBalanceId) ?.let { getVisitBalance(it) }
    ?: throw NotFoundException("Visit Balance $visitBalanceId not found")

  private fun getVisitBalance(latestBooking: OffenderBooking): VisitBalanceDetailResponse? = latestBooking.visitBalance?.let {
    val lastBatchIEPAdjustmentDate = latestBooking.visitBalanceAdjustments
      .filter { it.isIEPAllocation() && it.isCreatedByBatchVOProcess() }.maxByOrNull { it.adjustDate }?.adjustDate

    VisitBalanceDetailResponse(
      prisonNumber = latestBooking.offender.nomsId,
      remainingVisitOrders = latestBooking.visitBalance?.remainingVisitOrders!!,
      remainingPrivilegedVisitOrders = latestBooking.visitBalance?.remainingPrivilegedVisitOrders!!,
      lastIEPAllocationDate = lastBatchIEPAdjustmentDate
        ?: latestBooking.visitBalanceAdjustments.filter { it.isIEPAllocation() }
          .maxByOrNull { it.adjustDate }?.adjustDate,
    )
  }

  fun getVisitBalanceForPrisoner(offenderNo: String): VisitBalanceResponse? {
    val offenderBooking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Prisoner with offender no $offenderNo not found with any bookings")
    return offenderBooking.visitBalanceWithEntries()
  }

  fun OffenderBooking.visitBalanceWithEntries(): VisitBalanceResponse? = visitBalance ?.let {
    if (it.remainingVisitOrders != null && it.remainingPrivilegedVisitOrders != null) {
      VisitBalanceResponse(
        remainingVisitOrders = it.remainingVisitOrders!!,
        remainingPrivilegedVisitOrders = it.remainingPrivilegedVisitOrders!!,
      )
    } else {
      log.info("Visit balance for offender ${it.offenderBooking.offender.nomsId}, booking ${it.offenderBooking.bookingId} contains null entries")
      null
    }
  }

  fun getVisitBalanceAdjustment(visitBalanceAdjustmentId: Long): VisitBalanceAdjustmentResponse = offenderVisitBalanceAdjustmentRepository.findById(visitBalanceAdjustmentId)
    .map { it.toVisitBalanceAdjustmentResponse() }
    .orElseThrow { NotFoundException("Visit balance adjustment with id $visitBalanceAdjustmentId not found") }
}

fun OffenderVisitBalanceAdjustment.isCreatedByBatchVOProcess() = createUsername == "OMS_OWNER"
fun OffenderVisitBalanceAdjustment.isIEPAllocation() = adjustReasonCode.code == IEP_ENTITLEMENT
