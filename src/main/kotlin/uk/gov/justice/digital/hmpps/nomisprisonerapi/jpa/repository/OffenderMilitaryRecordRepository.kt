package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMilitaryRecord
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMilitaryRecordId

@Repository
interface OffenderMilitaryRecordRepository : JpaRepository<OffenderMilitaryRecord, OffenderMilitaryRecordId> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT omr FROM OffenderMilitaryRecord omr WHERE omr.id.offenderBooking.bookingId = :bookingId AND omr.id.sequence = :militarySeq")
  fun findByBookingIdAndMilitarySeqWithLock(bookingId: Long, militarySeq: Int): OffenderMilitaryRecord?

  @Query("SELECT omr FROM OffenderMilitaryRecord omr WHERE omr.id.offenderBooking.bookingId = :bookingId")
  fun findAllByBookingId(bookingId: Long): List<OffenderMilitaryRecord>
}
