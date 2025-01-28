package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance

@DslMarker
annotation class VisitBalanceDslMarker

@NomisDataDslMarker
interface VisitBalanceDsl

@Component
class VisitBalanceBuilderFactory {
  fun builder() = VisitBalanceBuilder()
}

class VisitBalanceBuilder : VisitBalanceDsl {
  fun build(
    offenderBooking: OffenderBooking,
    remainingVisitOrders: Int,
    remainingPrivilegedVisitOrders: Int,
  ): OffenderVisitBalance = OffenderVisitBalance(
    offenderBooking = offenderBooking,
    remainingVisitOrders = remainingVisitOrders,
    remainingPrivilegedVisitOrders = remainingPrivilegedVisitOrders,
    visitAllowanceIndicator = false,
  )
}
