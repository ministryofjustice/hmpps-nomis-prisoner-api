package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDateTime

@Repository
interface CourtCaseRepository : JpaRepository<CourtCase, Long> {
  fun findByOffenderBookingOffenderNomsIdOrderByCreateDatetimeDesc(
    nomsId: String,
  ): List<CourtCase>

  fun findByOffenderBookingOrderByCreateDatetimeDesc(
    offenderBooking: OffenderBooking,
  ): List<CourtCase>

  @Query("select coalesce(max(caseSequence), 0) + 1 from CourtCase where offenderBooking = :offenderBooking")
  fun getNextCaseSequence(offenderBooking: OffenderBooking): Int

  @Query(
    """
      select 
        courtCase.id
      from CourtCase courtCase 
        where 
          (:fromDateTime is null or courtCase.createDatetime > :fromDateTime) and 
          (:toDateTime is null or courtCase.createDatetime < :toDateTime)  
      order by courtCase.id asc
    """,
  )
  fun findAllCourtCaseIds(
    fromDateTime: LocalDateTime?,
    toDateTime: LocalDateTime?,
    pageable: Pageable,
  ): Page<Long>

  @Query(
    """
      select
        courtCase.id
      from CourtCase courtCase 
      order by courtCase.id asc
    """,
  )
  fun findAllCourtCaseIds(pageable: Pageable): Page<Long>
}
