package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderKeyDateAdjustment

@Repository
interface OffenderKeyDateAdjustmentRepository :
  CrudRepository<OffenderKeyDateAdjustment, Long>,
  JpaSpecificationExecutor<OffenderKeyDateAdjustment> {
  fun findByOffenderBookingAndActive(offenderBooking: OffenderBooking, isOffenderActive: Boolean): List<OffenderKeyDateAdjustment>
  fun findByOffenderBooking(offenderBooking: OffenderBooking): List<OffenderKeyDateAdjustment>
}
