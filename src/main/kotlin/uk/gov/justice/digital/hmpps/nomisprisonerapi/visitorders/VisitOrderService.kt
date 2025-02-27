package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitorders

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.time.LocalDate

@Service
@Transactional
class VisitOrderService(
  val offenderBookingRepository: OffenderBookingRepository,
) {
  fun getVisitOrderBalance(offenderNo: String): PrisonerVisitOrderBalanceResponse = offenderBookingRepository.findAllByOffenderNomsId(offenderNo).takeIf { it.isNotEmpty() }
    ?.let { bookings ->
      val toDate = LocalDate.now()
      val fromDate = toDate.minusMonths(1)
      val latestBooking = bookings.first { it.bookingSequence == 1 }

      PrisonerVisitOrderBalanceResponse(
        remainingVisitOrders = latestBooking.visitBalance?.remainingVisitOrders ?: 0,
        remainingPrivilegedVisitOrders = latestBooking.visitBalance?.remainingPrivilegedVisitOrders ?: 0,
        visitOrderBalanceAdjustments = latestBooking.visitBalanceAdjustments.filter {
          it.adjustDate?.let { adjDate -> !adjDate.isBefore(fromDate) && !adjDate.isAfter(toDate) } ?: false
        }.map { it.toVisitOrderBalanceAdjResponse() },
      )
    } ?: throw NotFoundException("Prisoner with offender no $offenderNo not found with any bookings")
}
