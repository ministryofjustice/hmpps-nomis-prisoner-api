package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents.IncidentsCount
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

  @Query(
    """
      select distinct(incident.location.id)
      from Incident incident 
      order by incident.location.id 
    """,
  )
  fun findAllIncidentAgencyLocations(): List<String>

  @Query(
    """
      select new uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents.IncidentsCount(
        coalesce(sum(case when incident.status.code in (:openStatusValues) THEN 1 else 0 end), 0),
        coalesce(sum(case when incident.status.code in (:closedStatusValues) THEN 1 else 0 end), 0)
      )
      from Incident incident 
      where incident.location.id = :agencyLocationId
      order by incident.location.id
    """,
  )
  fun countsByAgencyLocationId(agencyLocationId: String, openStatusValues: List<String>, closedStatusValues: List<String>): IncidentsCount
}
