package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlert
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAlertId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDateTime

@Repository
interface OffenderAlertRepository : JpaRepository<OffenderAlert, OffenderAlertId> {
  @Query("select coalesce(max(id.sequence), 0) + 1 from OffenderAlert where id.offenderBooking = :offenderBooking")
  fun getNextSequence(offenderBooking: OffenderBooking): Long

  @Suppress("ktlint:standard:function-naming")
  fun findById_OffenderBookingAndId_Sequence(offenderBooking: OffenderBooking, alertSequence: Long): OffenderAlert?

  @Suppress("ktlint:standard:function-naming")
  fun findAllById_OffenderBooking(offenderBooking: OffenderBooking): List<OffenderAlert>

  @Query(
    """
      select 
        alert.id.offenderBooking.bookingId as bookingId, 
        alert.id.sequence as alertSequence, 
        alert.id.offenderBooking.offender.nomsId as offenderNo
      from OffenderAlert alert 
        order by alert.id.offenderBooking.bookingId, alert.id.sequence asc
    """,
  )
  fun findAllAlertIds(
    pageable: Pageable,
  ): Page<AlertId>

  @Query(
    """
      select 
        alert.id.offenderBooking.bookingId as bookingId, 
        alert.id.sequence as alertSequence, 
        alert.id.offenderBooking.offender.nomsId as offenderNo
      from OffenderAlert alert 
        where 
          (:fromDate is null or alert.createDatetime > :fromDate) and 
          (:toDate is null or alert.createDatetime < :toDate)    
        order by alert.id.offenderBooking.bookingId, alert.id.sequence asc
    """,
  )
  fun findAllAlertIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<AlertId>
}
interface AlertId {
  fun getBookingId(): Long
  fun getAlertSequence(): Long
  fun getOffenderNo(): String
}
