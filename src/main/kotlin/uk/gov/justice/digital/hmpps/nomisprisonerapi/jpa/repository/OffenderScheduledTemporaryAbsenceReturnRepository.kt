package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn

@Repository
interface OffenderScheduledTemporaryAbsenceReturnRepository : JpaRepository<OffenderScheduledTemporaryAbsenceReturn, Long> {
  @Suppress("ktlint:standard:function-naming")
  fun findByEventIdAndOffenderBooking_Offender_NomsId(eventId: Long, nomsId: String): OffenderScheduledTemporaryAbsenceReturn?
}
