package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventChargeId

@Repository
interface CourtEventChargeRepository : JpaRepository<CourtEventCharge, CourtEventChargeId> {

  fun existsByIdOffenderChargeId(offenderChargeId: Long): Boolean

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "1000")])
  @Query("SELECT c FROM CourtEventCharge c WHERE c.id = :id")
  fun findByIdOrNullWaitForLock(id: CourtEventChargeId): CourtEventCharge?

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "1000")])
  @Query("SELECT c FROM CourtEventCharge c WHERE c.id = :id")
  fun findByIdWaitForLock(id: CourtEventChargeId): CourtEventCharge

  @Query(
    """
      select
        charge.id.offenderCharge.id as offenderChargeId,
        charge.id.courtEvent.id as eventId
      from CourtEventCharge charge
      where charge.propertyValue is not null
      order by charge.id.offenderCharge.id, charge.id.courtEvent.id asc
    """,
  )
  fun findAllIdsWithPropertyValue(pageable: Pageable): Page<CourtEventChargeIdProjection>

  // keyset (seek) pagination - avoids the expensive COUNT query and increasingly slow OFFSET scans
  // that findAllIdsWithPropertyValue incurs on a large table. Callers should iterate by passing
  // the last offenderChargeId/eventId seen (0, 0 for the first call) until fewer than pageSize rows
  // are returned.
  @Query(
    """
      select * from (
        select
          OFFENDER_CHARGE_ID as offenderChargeId,
          EVENT_ID as eventId
        from COURT_EVENT_CHARGES
        where PROPERTY_VALUE is not null
          and (
            OFFENDER_CHARGE_ID > :offenderChargeId
            or (OFFENDER_CHARGE_ID = :offenderChargeId and EVENT_ID > :eventId)
          )
        order by OFFENDER_CHARGE_ID, EVENT_ID
      )
      where rownum <= :pageSize
    """,
    nativeQuery = true,
  )
  fun findAllIdsWithPropertyValueFromId(offenderChargeId: Long, eventId: Long, pageSize: Int): List<CourtEventChargeIdProjection>
}

interface CourtEventChargeIdProjection {
  fun getOffenderChargeId(): Long
  fun getEventId(): Long
}
