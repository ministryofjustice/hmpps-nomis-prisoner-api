package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalance

@Repository
interface OffenderVisitBalanceRepository : CrudRepository<OffenderVisitBalance, Long> {
  @Query(
    value = """
      select ovb
        from OffenderVisitBalance ovb join ovb.offenderBooking ob
        where ob.bookingSequence = 1
        and (:prisonId is null or ob.location.id = :prisonId)
    """,
    countQuery = """
      select count(ovb) from OffenderVisitBalance ovb join ovb.offenderBooking ob
        where ob.bookingSequence = 1 
        and (:prisonId is null or ob.location.id = :prisonId)
    """,
  )
  fun findForLatestBooking(prisonId: String? = null, pageable: Pageable): Page<OffenderVisitBalance>

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "20000")])
  fun findByIdForUpdate(offenderBookingId: Long): OffenderVisitBalance? = findByIdOrNull(offenderBookingId)
}
