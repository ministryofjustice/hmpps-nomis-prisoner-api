package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceTerm
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceTermId

@Repository
interface OffenderSentenceTermRepository : JpaRepository<OffenderSentenceTerm, OffenderSentenceTermId> {
  @Query(value = "SELECT NVL(MAX(TERM_SEQ)+1, 1) FROM OFFENDER_SENTENCE_TERMS oos WHERE OFFENDER_BOOK_ID = :offenderBookId and SENTENCE_SEQ = :sentenceSeq", nativeQuery = true)
  fun getNextTermSequence(offenderBookId: Long, sentenceSeq: Long): Long

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000")])
  @Query("SELECT st FROM OffenderSentenceTerm st WHERE st.id = :id")
  fun findByIdOrNullForUpdate(id: OffenderSentenceTermId): OffenderSentenceTerm?
}
