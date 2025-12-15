package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface VisitRepository :
  JpaSpecificationExecutor<Visit>,
  JpaRepository<Visit, Long>,
  VisitCustomRepository {
  fun findByOffenderBooking(booking: OffenderBooking): List<Visit>

  @Suppress("FunctionName")
  fun findByIdAndOffenderBooking_Offender_NomsId(visitId: Long, offenderNo: String): Optional<Visit>

  fun existsByOffenderBookingAndStartDateTimeAndEndDateTimeAndCommentTextAndVisitStatusAndAgencyInternalLocation(
    offenderBooking: OffenderBooking,
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    commentText: String?,
    visitStatus: VisitStatus,
    room: AgencyInternalLocation?,
  ): Boolean

  fun findByOffenderBookingAndStartDateTimeAndEndDateTimeAndCommentTextAndVisitStatusAndAgencyInternalLocation(
    offenderBooking: OffenderBooking,
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime,
    commentText: String?,
    visitStatus: VisitStatus,
    room: AgencyInternalLocation?,
  ): Visit?

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(value = [QueryHint(name = "jakarta.persistence.lock.timeout", value = "20000")])
  fun findByIdForUpdate(visitId: Long): Visit? = findByIdOrNull(visitId)

  @Query(
    """
      select 
        v.id as id
      from Visit v 
      where v.visitType.code = 'OFFI'
    """,
  )
  fun findAllOfficialVisitsIds(
    pageable: Pageable,
  ): Page<VisitIdProjection>

  @Query(
    """
      select *
      from (select v.OFFENDER_VISIT_ID as id
            from OFFENDER_VISITS v
            where v.VISIT_TYPE = 'OFFI'
              and v.OFFENDER_VISIT_ID > :visitId
            order by v.OFFENDER_VISIT_ID)
      where rownum <= :pageSize
    """,
    nativeQuery = true,
  )
  fun findAllOfficialVisitsIds(
    visitId: Long,
    pageSize: Int,
  ): List<VisitIdProjection>

  @Query(
    """
      select 
        v.id as id
      from Visit v 
      where v.visitType.code = 'OFFI'
      and 
          (:fromDate is null or v.createDatetime > :fromDate) and 
          (:toDate is null or v.createDatetime < :toDate)    
  """,
  )
  fun findAllOfficialVisitsIdsWithDateFilter(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<VisitIdProjection>

  @Query(
    """
      select *
      from (select v.OFFENDER_VISIT_ID as id
            from OFFENDER_VISITS v
            where v.VISIT_TYPE = 'OFFI'
              and v.OFFENDER_VISIT_ID > :visitId
              and (:fromDate is null or v.CREATE_DATETIME > :fromDate)  
              and (:toDate is null or v.CREATE_DATETIME < :toDate)   
            order by v.OFFENDER_VISIT_ID)
      where rownum <= :pageSize
  """,
    nativeQuery = true,
  )
  fun findAllOfficialVisitsIdsWithDateFilter(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    visitId: Long,
    pageSize: Int,
  ): List<VisitIdProjection>

  @Query(
    """
      select 
        v.id as id
      from Visit v 
      where v.visitType.code = 'OFFI'
      and 
        (:fromDate is null or v.createDatetime > :fromDate) and 
        (:toDate is null or v.createDatetime < :toDate)  and 
        v.location.id in (:prisonIds)    
    """,
  )
  fun findAllOfficialVisitsIdsWithDateAndPrisonFilter(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    prisonIds: List<String>,
    pageable: Pageable,
  ): Page<VisitIdProjection>

  @Query(
    """
      select *
      from (select v.OFFENDER_VISIT_ID as id
            from OFFENDER_VISITS v
            where v.VISIT_TYPE = 'OFFI'
              and v.OFFENDER_VISIT_ID > :visitId
              and v.AGY_LOC_ID in (:prisonIds)
              and (:fromDate is null or v.CREATE_DATETIME > :fromDate)  
              and (:toDate is null or v.CREATE_DATETIME < :toDate)   
            order by v.OFFENDER_VISIT_ID)
      where rownum <= :pageSize
  """,
    nativeQuery = true,
  )
  fun findAllOfficialVisitsIdsWithDateAndPrisonFilter(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    prisonIds: List<String>,
    visitId: Long,
    pageSize: Int,
  ): List<VisitIdProjection>
}

interface VisitIdProjection {
  val id: Long
}
