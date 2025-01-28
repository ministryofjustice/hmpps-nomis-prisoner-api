package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentChargeId
import java.time.LocalDateTime

@Repository
interface AdjudicationIncidentChargeRepository : JpaRepository<AdjudicationIncidentCharge, AdjudicationIncidentChargeId> {

  @Query(
    """
      select 
        charge.incidentParty.adjudicationNumber as adjudicationNumber, 
        charge.id.chargeSequence as chargeSequence, 
        charge.incidentParty.offenderBooking.offender.nomsId as nomsId
      from AdjudicationIncidentCharge charge 
        where 
          (:fromDate is null or charge.whenCreated > :fromDate) and 
          (:toDate is null or charge.whenCreated < :toDate) and   
          (:hasPrisonFilter = false or charge.incident.prison.id in :prisonIds) 
        order by charge.incident.id, charge.id.chargeSequence asc
    """,
  )
  fun findAllAdjudicationChargeIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    prisonIds: List<String>?,
    hasPrisonFilter: Boolean,
    pageable: org.springframework.data.domain.Pageable,
  ): Page<AdjudicationChargeId>

  @Query(
    """
      select 
        charge.incidentParty.adjudicationNumber as adjudicationNumber, 
        charge.id.chargeSequence as chargeSequence, 
        charge.incidentParty.offenderBooking.offender.nomsId as nomsId
      from AdjudicationIncidentCharge charge 
        where 
          charge.incident.prison.id in :prisonIds 
        order by charge.incident.id, charge.id.chargeSequence asc
    """,
  )
  fun findAllAdjudicationChargeIds(
    prisonIds: List<String>?,
    pageable: org.springframework.data.domain.Pageable,
  ): Page<AdjudicationChargeId>

  @Query(
    """
      select 
        charge.incidentParty.adjudicationNumber as adjudicationNumber, 
        charge.id.chargeSequence as chargeSequence, 
        charge.incidentParty.offenderBooking.offender.nomsId as nomsId
      from AdjudicationIncidentCharge charge 
        order by charge.incident.id, charge.id.chargeSequence asc
    """,
  )
  fun findAllAdjudicationChargeIds(
    pageable: org.springframework.data.domain.Pageable,
  ): Page<AdjudicationChargeId>
}

interface AdjudicationChargeId {
  fun getAdjudicationNumber(): Long
  fun getChargeSequence(): Int
  fun getNomsId(): String
}
