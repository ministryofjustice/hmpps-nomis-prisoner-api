package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReview

@Repository
interface CSIPReviewRepository : CrudRepository<CSIPReview, Long>, JpaSpecificationExecutor<CSIPReview> {
  @Query("select coalesce(max(reviewSequence), 0) + 1 from CSIPReview where csipReport.id = :reportId")
  fun getNextSequence(reportId: Long): Int
}
