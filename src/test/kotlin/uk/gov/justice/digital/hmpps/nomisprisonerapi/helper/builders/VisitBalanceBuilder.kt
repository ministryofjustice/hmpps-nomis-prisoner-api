package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance

class VisitBalanceBuilder(
  var remainingVisitOrders: Int = 4,
  var remainingPrivilegedVisitOrders: Int = 3,
) {
  fun build(offenderBooking: OffenderBooking): OffenderVisitBalance = OffenderVisitBalance(
    offenderBooking = offenderBooking,
    remainingVisitOrders = remainingVisitOrders,
    remainingPrivilegedVisitOrders = remainingPrivilegedVisitOrders,
    visitAllowanceIndicator = null,
  )
}
