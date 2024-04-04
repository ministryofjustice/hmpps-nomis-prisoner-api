package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MergeTransaction
import java.time.LocalDateTime

@Repository
interface MergeTransactionRepository : JpaRepository<MergeTransaction, Long> {
  @Query("select m from MergeTransaction m where (m.nomsId1 = :offenderNo or m.nomsId2 = :offenderNo) and (:requestDate is null or m.requestDate > :requestDate)")
  fun findByNomsIdAndRequestDateGreaterThanEqual(offenderNo: String, requestDate: LocalDateTime?): List<MergeTransaction>
}
