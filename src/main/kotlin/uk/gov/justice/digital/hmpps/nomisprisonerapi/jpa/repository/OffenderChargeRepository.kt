package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge

@Repository
interface OffenderChargeRepository : JpaRepository<OffenderCharge, Long> {
  fun deleteByOffenderBookingBookingId(bookingId: Long)

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000")])
  @Query("SELECT c FROM OffenderCharge c WHERE c.id = :id")
  fun findByIdOrNullForUpdate(id: Long): OffenderCharge?
}
