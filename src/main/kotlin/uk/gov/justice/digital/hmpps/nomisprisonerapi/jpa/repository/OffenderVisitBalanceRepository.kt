package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance

@Repository
interface OffenderVisitBalanceRepository : CrudRepository<OffenderVisitBalance, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "20000")])
  @Query("SELECT ovb FROM OffenderVisitBalance ovb WHERE ovb.offenderBookingId = :offenderBookingId")
  fun findByIdForUpdate(offenderBookingId: Long): OffenderVisitBalance?
}
