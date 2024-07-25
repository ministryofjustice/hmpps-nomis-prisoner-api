package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import java.time.LocalDateTime

@Repository
interface CSIPReportRepository : CrudRepository<CSIPReport, Long>, JpaSpecificationExecutor<CSIPReport> {
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
}
