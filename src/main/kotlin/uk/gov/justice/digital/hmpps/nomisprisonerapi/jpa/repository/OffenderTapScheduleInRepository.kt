package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn

@Repository
interface OffenderTapScheduleInRepository : JpaRepository<OffenderTapScheduleIn, Long> {
  @Suppress("ktlint:standard:function-naming")
  fun findByEventIdAndOffenderBooking_Offender_NomsId(eventId: Long, nomsId: String): OffenderTapScheduleIn?
}
