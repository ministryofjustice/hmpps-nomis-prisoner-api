package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence

@Repository
interface OffenderScheduledTemporaryAbsenceRepository : JpaRepository<OffenderScheduledTemporaryAbsence, Long> {
  @Suppress("ktlint:standard:function-naming")
  fun findByEventIdAndOffenderBooking_Offender_NomsId(eventId: Long, offenderNo: String): OffenderScheduledTemporaryAbsence?
}
