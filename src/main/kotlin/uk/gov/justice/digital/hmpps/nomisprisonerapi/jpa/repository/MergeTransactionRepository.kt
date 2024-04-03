package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MergeTransaction
import java.time.LocalDateTime

@Repository
interface MergeTransactionRepository : JpaRepository<MergeTransaction, Long> {
  fun findByNomsId1AndRequestDateGreaterThanEqual(offenderNo: String, localDateTime: LocalDateTime?): List<MergeTransaction>
  fun findByNomsId1(offenderNo: String): List<MergeTransaction>
}
