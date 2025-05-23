package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceRepository
import java.time.LocalDate

@DslMarker
annotation class VisitBalanceDslMarker

@NomisDataDslMarker
interface VisitBalanceDsl {
  @VisitBalanceAdjustmentDslMarker
  fun visitBalanceAdjustment(
    visitOrderChange: Int? = 4,
    previousVisitOrderCount: Int? = 0,
    privilegedVisitOrderChange: Int? = 3,
    previousPrivilegedVisitOrderCount: Int? = 0,
    adjustmentDate: LocalDate = LocalDate.parse("2022-01-01"),
    adjustmentReasonCode: String = IEP_ENTITLEMENT,
    comment: String? = null,
    expiryBalance: Int? = null,
    expiryDate: LocalDate? = null,
    endorsedStaffId: Long? = null,
    authorisedStaffId: Long,
    dsl: VisitBalanceAdjustmentDsl.() -> Unit = {},
  ): OffenderVisitBalanceAdjustment
}

@Component
class VisitBalanceBuilderFactory(
  private val repository: VisitBalanceBuilderRepository,
  private val visitBalanceAdjustmentBuilderFactory: VisitBalanceAdjustmentBuilderFactory,
) {
  fun builder() = VisitBalanceBuilder(repository, visitBalanceAdjustmentBuilderFactory)
}

@Component
class VisitBalanceBuilderRepository(
  private val visitBalanceRepository: OffenderVisitBalanceRepository,
) {
  fun save(visitBalance: OffenderVisitBalance): OffenderVisitBalance = visitBalanceRepository.save(visitBalance)
}

class VisitBalanceBuilder(
  private val repository: VisitBalanceBuilderRepository,
  private val visitBalanceAdjustmentBuilderFactory: VisitBalanceAdjustmentBuilderFactory,
) : VisitBalanceDsl {

  private lateinit var visitBalance: OffenderVisitBalance

  fun build(
    offenderBooking: OffenderBooking,
    remainingVisitOrders: Int?,
    remainingPrivilegedVisitOrders: Int?,
  ): OffenderVisitBalance = OffenderVisitBalance(
    offenderBooking = offenderBooking,
    remainingVisitOrders = remainingVisitOrders,
    remainingPrivilegedVisitOrders = remainingPrivilegedVisitOrders,
    visitAllowanceIndicator = false,
  )
    .also { visitBalance = it }
    .also { offenderBooking.visitBalance = it }

  override fun visitBalanceAdjustment(
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
    authorisedStaffId: Long,
    dsl: VisitBalanceAdjustmentDsl.() -> Unit,
  ): OffenderVisitBalanceAdjustment = visitBalanceAdjustmentBuilderFactory.builder()
    .let { builder ->
      builder.build(
        visitBalance = visitBalance,
        visitOrderChange = visitOrderChange,
        previousVisitOrderCount = previousVisitOrderCount,
        privilegedVisitOrderChange = privilegedVisitOrderChange,
        previousPrivilegedVisitOrderCount = previousPrivilegedVisitOrderCount,
        adjustmentDate = adjustmentDate,
        adjustmentReasonCode = adjustmentReasonCode,
        comment = comment,
        expiryBalance = expiryBalance,
        expiryDate = expiryDate,
        endorsedStaffId = endorsedStaffId,
        authorisedStaffId = authorisedStaffId,
      ).also {
        visitBalance.visitBalanceAdjustments += it
        builder.apply(dsl)
      }
    }
}
