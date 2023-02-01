package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderKeyDateAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing.AdjustmentIdResponse
import java.time.LocalDate

@Repository
interface OffenderKeyDateAdjustmentRepository :
  CrudRepository<OffenderKeyDateAdjustment, Long>,
  JpaSpecificationExecutor<OffenderKeyDateAdjustment> {
  @Query(nativeQuery = true)
  fun adjustmentIdsQuery_named(fromDate: LocalDate? = null, toDate: LocalDate? = null, pageable: Pageable): Page<AdjustmentIdResponse>
}
