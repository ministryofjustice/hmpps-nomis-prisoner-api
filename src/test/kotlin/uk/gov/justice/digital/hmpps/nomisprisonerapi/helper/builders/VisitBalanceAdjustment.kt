package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class VisitBalanceAdjustmentDslMarker

@NomisDataDslMarker
interface VisitBalanceAdjustmentDsl

@Component
class VisitBalanceAdjustmentBuilderFactory(
  private val repository: VisitBalanceAdjustmentBuilderRepository,
) {
  fun builder() = VisitBalanceAdjustmentBuilder(repository)
}

@Component
class VisitBalanceAdjustmentBuilderRepository(
  val adjustmentReasonRepository: ReferenceCodeRepository<VisitOrderAdjustmentReason>,
) {
  fun lookupRole(code: String) = adjustmentReasonRepository.findByIdOrNull(VisitOrderAdjustmentReason.pk(code))!!
}

class VisitBalanceAdjustmentBuilder(
  private val repository: VisitBalanceAdjustmentBuilderRepository,
) : VisitBalanceAdjustmentDsl {
  fun build(
    offenderBooking: OffenderBooking,
    visitOrderChange: Int?,
    previousVisitOrderCount: Int?,
    privilegedVisitOrderChange: Int?,
    previousPrivilegedVisitOrderCount: Int?,
    adjustmentDate: LocalDate,
    adjustmentReasonCode: String,
    comment: String?,
    expiryBalance: Int?,
    expiryDate: LocalDate?,
    endorsedStaffId: Long?,
    authorisedStaffId: Long?,
  ): OffenderVisitBalanceAdjustment = OffenderVisitBalanceAdjustment(
    offenderBooking = offenderBooking,
    remainingVisitOrders = visitOrderChange,
    previousRemainingVisitOrders = previousVisitOrderCount,
    remainingPrivilegedVisitOrders = privilegedVisitOrderChange,
    previousRemainingPrivilegedVisitOrders = previousPrivilegedVisitOrderCount,
    adjustDate = adjustmentDate,
    adjustReasonCode = repository.lookupRole(adjustmentReasonCode),
    commentText = comment,
    expiryBalance = expiryBalance,
    expiryDate = expiryDate,
    endorsedStaffId = endorsedStaffId,
    authorisedStaffId = authorisedStaffId,
  )
}
