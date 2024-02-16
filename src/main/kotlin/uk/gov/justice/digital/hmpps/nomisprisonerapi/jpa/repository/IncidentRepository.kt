package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import java.time.LocalDateTime

@Repository
interface IncidentRepository : CrudRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {
  @Query(
    """
      select 
        incident.id
      from Incident incident 
        where 
          (:fromDate is null or incident.createDatetime > :fromDate) and 
          (:toDate is null or incident.createDatetime < :toDate)  
      order by incident.id asc
    """,
  )
  fun findAllIncidentIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<Long>

  @Query(
    """
      select
        incident.id
      from Incident incident 
      order by incident.id asc
    """,
  )
  fun findAllIncidentIds(pageable: Pageable): Page<Long>
}
