package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderKeyDateAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing.AdjustmentIdResponse
import java.time.LocalDate

@Repository
interface OffenderKeyDateAdjustmentRepository :
  CrudRepository<OffenderKeyDateAdjustment, Long>,
  JpaSpecificationExecutor<OffenderKeyDateAdjustment> {
  @Query(nativeQuery = true)
  fun adjustmentIdsQueryNamed(fromDate: LocalDate? = null, toDate: LocalDate? = null, pageable: Pageable): Page<AdjustmentIdResponse>
  fun findByOffenderBookingAndActive(offenderBooking: OffenderBooking, isOffenderActive: Boolean): List<OffenderKeyDateAdjustment>
  fun findByOffenderBooking(offenderBooking: OffenderBooking): List<OffenderKeyDateAdjustment>
}
