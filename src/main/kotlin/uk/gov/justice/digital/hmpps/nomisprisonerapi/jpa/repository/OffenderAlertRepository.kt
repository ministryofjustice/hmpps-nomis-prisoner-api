package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlert
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlertId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

@Repository
interface OffenderAlertRepository : JpaRepository<OffenderAlert, OffenderAlertId> {
  @Query("select coalesce(max(id.sequence), 0) + 1 from OffenderAlert where id.offenderBooking = :offenderBooking")
  fun getNextSequence(offenderBooking: OffenderBooking): Int

  @Suppress("ktlint:standard:function-naming")
  fun findById_OffenderBookingAndId_Sequence(offenderBooking: OffenderBooking, alertSequence: Long): OffenderAlert?
}
