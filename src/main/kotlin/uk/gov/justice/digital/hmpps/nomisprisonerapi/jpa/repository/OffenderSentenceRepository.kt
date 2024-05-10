package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId

@Repository
interface OffenderSentenceRepository :
  JpaRepository<OffenderSentence, SentenceId>,
  JpaSpecificationExecutor<OffenderSentence> {

  fun deleteByIdOffenderBookingBookingId(bookingId: Long)

  @Query("select coalesce(max(id.sequence), 0) + 1 from OffenderSentence where id.offenderBooking = :offenderBooking")
  fun getNextSequence(offenderBooking: OffenderBooking): Long
}
