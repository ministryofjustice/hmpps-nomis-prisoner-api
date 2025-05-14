package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.trackEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.PVO_ISSUE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.VO_ISSUE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances.VisitBalanceService.Companion.OMS_OWNER

@Service
@Transactional
class VisitBalanceService(
  private val visitBalanceRepository: OffenderVisitBalanceRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository,
  private val visitOrderAdjustmentReasonRepository: ReferenceCodeRepository<VisitOrderAdjustmentReason>,
  private val staffUserAccountRepository: StaffUserAccountRepository,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val OMS_OWNER = "OMS_OWNER"
  }
  fun findAllIds(prisonId: String?, pageRequest: Pageable): Page<VisitBalanceIdResponse> = visitBalanceRepository.findForLatestBooking(prisonId, pageRequest)
    .map { VisitBalanceIdResponse(it.offenderBookingId) }

  fun getVisitBalanceById(visitBalanceId: Long): VisitBalanceDetailResponse = offenderBookingRepository.findByIdOrNull(visitBalanceId) ?.let { getVisitBalanceForPrisoner(it) }
    ?: throw NotFoundException("Visit Balance $visitBalanceId not found")

  private fun getVisitBalanceForPrisoner(latestBooking: OffenderBooking): VisitBalanceDetailResponse? = latestBooking.visitBalance?.let {
    val lastBatchIEPAdjustmentDate = latestBooking.visitBalanceAdjustments
      .filter { it.isIEPAllocation() && it.isCreatedByBatchVOProcess() }.maxByOrNull { it.adjustDate }?.adjustDate
    VisitBalanceDetailResponse(
      prisonNumber = latestBooking.offender.nomsId,
      remainingVisitOrders = latestBooking.visitBalance?.remainingVisitOrders ?: 0,
      remainingPrivilegedVisitOrders = latestBooking.visitBalance?.remainingPrivilegedVisitOrders ?: 0,
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

  fun upsertVisitBalance(offenderNo: String, request: UpdateVisitBalanceRequest) {
    val offenderBooking = offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Prisoner with offender no $offenderNo not found with any bookings")

    offenderBooking.visitBalance?.let {
      if (
        it.remainingVisitOrders != request.remainingVisitOrders ||
        it.remainingPrivilegedVisitOrders != request.remainingPrivilegedVisitOrders
      ) {
        telemetryClient.trackEvent(
          "visit.balance.upsert.updated",
          mapOf(
            "prisonNumber" to offenderNo,
            "remainingVisitOrders" to request.remainingVisitOrders.toString(),
            "remainingPrivilegedVisitOrders" to request.remainingPrivilegedVisitOrders.toString(),
          ),
        )
        it.remainingVisitOrders = request.remainingVisitOrders
        it.remainingPrivilegedVisitOrders = request.remainingPrivilegedVisitOrders
      } else {
        telemetryClient.trackEvent("visit.balance.upsert.unchanged", mapOf("prisonNumber" to offenderNo))
      }
    } ?: let {
      // only take action if we get a non-null order
      if (request.remainingVisitOrders != null || request.remainingPrivilegedVisitOrders != null) {
        offenderBooking.visitBalance = OffenderVisitBalance(
          remainingVisitOrders = request.remainingVisitOrders,
          remainingPrivilegedVisitOrders = request.remainingPrivilegedVisitOrders,
          offenderBooking = offenderBooking,
        )
        telemetryClient.trackEvent(
          "visit.balance.upsert.inserted",
          mapOf(
            "prisonNumber" to offenderNo,
            "remainingVisitOrders" to request.remainingVisitOrders.toString(),
            "remainingPrivilegedVisitOrders" to request.remainingPrivilegedVisitOrders.toString(),
          ),
        )
      }
    }
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

  fun createVisitBalanceAdjustment(prisonNumber: String, request: CreateVisitBalanceAdjustmentRequest): CreateVisitBalanceAdjustmentResponse {
    val offenderBooking = offenderBookingRepository.findLatestByOffenderNomsId(prisonNumber)
      ?: throw NotFoundException("Prisoner $prisonNumber not found with a booking")
    val adjustmentReasonPk = if (request.visitOrderChange != null) VO_ISSUE else PVO_ISSUE
    val adjustmentReason = visitOrderAdjustmentReasonRepository.findById(adjustmentReasonPk)
      .orElseThrow { RuntimeException("Visit Adjustment Reason with code $adjustmentReasonPk does not exist") }
    val staffUserAccount = (request.authorisedUsername ?: OMS_OWNER).let {
      staffUserAccountRepository.findByUsername(it) ?: throw BadDataException("Username $it not found")
    }

    val visitBalanceAdjustment = OffenderVisitBalanceAdjustment(
      offenderBooking = offenderBooking,
      adjustDate = request.adjustmentDate,
      adjustReasonCode = adjustmentReason,
      remainingVisitOrders = request.visitOrderChange,
      previousRemainingVisitOrders = request.previousVisitOrderCount,
      remainingPrivilegedVisitOrders = request.privilegedVisitOrderChange,
      previousRemainingPrivilegedVisitOrders = request.previousPrivilegedVisitOrderCount,
      commentText = request.comment,
      authorisedStaffId = staffUserAccount.staff.id,
      endorsedStaffId = staffUserAccount.staff.id,
    )

    offenderBooking.visitBalanceAdjustments.add(visitBalanceAdjustment)
    return CreateVisitBalanceAdjustmentResponse(visitBalanceAdjustmentId = visitBalanceAdjustment.id)
  }
}

fun OffenderVisitBalanceAdjustment.isCreatedByBatchVOProcess() = createUsername == OMS_OWNER
fun OffenderVisitBalanceAdjustment.isIEPAllocation() = adjustReasonCode.code == IEP_ENTITLEMENT
