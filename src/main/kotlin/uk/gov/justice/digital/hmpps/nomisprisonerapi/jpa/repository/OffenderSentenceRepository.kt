package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceId

@Repository
interface OffenderSentenceRepository :
  JpaRepository<OffenderSentence, SentenceId>,
  JpaSpecificationExecutor<OffenderSentence> {

  fun findByIdAndCourtCaseId(sentenceId: SentenceId, courtCaseId: Long): OffenderSentence?

  @Query("select coalesce(max(id.sequence), 0) + 1 from OffenderSentence where id.offenderBooking = :offenderBooking")
  fun getNextSequence(offenderBooking: OffenderBooking): Long

  @Query("select coalesce(max(lineSequence), 0) + 1 from OffenderSentence where id.offenderBooking = :offenderBooking")
  fun getNextLineSequence(offenderBooking: OffenderBooking): Long

  fun findByIdOffenderBookingBookingIdInAndAuditModuleNameAndStatusAndCourtCaseIsNotNull(offenderBookingIds: List<Long>, auditModule: String = "MERGE", status: String = "I"): List<OffenderSentence>

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000")])
  @Query("SELECT s FROM OffenderSentence s WHERE s.id = :id")
  fun findByIdOrNullForUpdate(id: SentenceId): OffenderSentence?
}
