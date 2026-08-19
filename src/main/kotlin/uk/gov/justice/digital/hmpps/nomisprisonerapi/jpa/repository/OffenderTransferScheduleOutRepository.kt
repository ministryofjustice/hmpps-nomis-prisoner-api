@file:Suppress("ktlint:standard:function-naming")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut

@Repository
interface OffenderTransferScheduleOutRepository : JpaRepository<OffenderTransferScheduleOut, Long> {
  fun findAllByOffenderBooking_Offender_NomsId(offenderNo: String): List<OffenderTransferScheduleOut>
}
