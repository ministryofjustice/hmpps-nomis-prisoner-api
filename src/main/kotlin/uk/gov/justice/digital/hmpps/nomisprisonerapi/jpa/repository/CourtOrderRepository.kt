package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtOrder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

@Repository
interface CourtOrderRepository : JpaRepository<CourtOrder, Long> {
  fun findByOffenderBookingAndCourtEventAndOrderType(
    offenderBooking: OffenderBooking,
    courtEvent: CourtEvent,
    orderType: String = "AUTO",
  ): CourtOrder?
}
