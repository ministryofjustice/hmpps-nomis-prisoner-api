package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.BookingCountWithPay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourseAttendance
import java.time.LocalDate

@Repository
interface OffenderCourseAttendanceRepository : JpaRepository<OffenderCourseAttendance, Long> {
  fun findByCourseScheduleAndOffenderBooking(
    courseSchedule: CourseSchedule,
    offenderBooking: OffenderBooking,
  ): OffenderCourseAttendance?

  @Query(
    value = """
      select new uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.BookingCountWithPay(oca.offenderBooking.bookingId, count(oca), sum(otd.payAmount))
      from OffenderCourseAttendance oca
      join OffenderBooking ob on oca.offenderBooking = ob
      join OffenderTransactionDetail otd on oca.transaction = otd.transaction and oca.eventId = otd.courseAttendance.eventId
      where oca.prison.id = :prisonId
      and oca.eventDate = :date
      and oca.pay = true
      group by oca.offenderBooking.bookingId
      order by oca.offenderBooking.bookingId
    """,
  )
  fun findBookingPaidAttendanceCountsByPrisonAndDate(prisonId: String, date: LocalDate): List<BookingCountWithPay>
}
