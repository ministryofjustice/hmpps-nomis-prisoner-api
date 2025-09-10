package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import java.time.LocalDateTime

@Repository
interface CSIPReportRepository :
  CrudRepository<CSIPReport, Long>,
  JpaSpecificationExecutor<CSIPReport> {
  @Query(
    """
      select 
        csipReport.id
      from CSIPReport csipReport 
        where 
          (:fromDate is null or csipReport.createDatetime > :fromDate) and 
          (:toDate is null or csipReport.createDatetime < :toDate)  
      order by csipReport.id asc
    """,
  )
  fun findAllCSIPIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<Long>

  @Query(
    """
      select
        csipReport.id
      from CSIPReport csipReport 
      order by csipReport.id asc
    """,
  )
  fun findAllCSIPIds(pageable: Pageable): Page<Long>

  fun findAllByOffenderBookingOffenderNomsId(offenderNo: String): List<CSIPReport>

  @Query(
    """
      select
        csipReport.id
      from CSIPReport csipReport 
         where
            csipReport.offenderBooking.bookingId = :bookingId
      order by csipReport.id asc
    """,
  )
  fun findIdsByOffenderBookingBookingId(bookingId: Long): List<Long>

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000")])
  @Query("SELECT c FROM CSIPReport c WHERE c.id = :csipId")
  fun findByIdOrNullForUpdate(csipId: Long): CSIPReport?
}
