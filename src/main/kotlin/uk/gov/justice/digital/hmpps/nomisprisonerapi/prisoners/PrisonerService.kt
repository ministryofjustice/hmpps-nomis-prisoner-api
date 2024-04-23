package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.MergeTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.findLatestAliasByNomisId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.ActiveBookingsSpecification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.OffenderWithBookingsSpecification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.status
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class PrisonerService(
  private val bookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
  private val mergeTransactionRepository: MergeTransactionRepository,
) {
  fun findAllActivePrisoners(pageRequest: Pageable): Page<PrisonerId> {
    return bookingRepository.findAll(ActiveBookingsSpecification(), pageRequest)
      .map { PrisonerId(bookingId = it.bookingId, offenderNo = it.offender.nomsId, status = it.status()) }
  }

  fun findAllPrisonersWithBookings(pageRequest: Pageable): Page<PrisonerId> =
    offenderRepository.findAll(
      OffenderWithBookingsSpecification(),
      PageRequest.of(
        pageRequest.pageNumber,
        pageRequest.pageSize,
        Sort.by(ASC, "rootOffenderId"),
      ),
    ).map {
      PrisonerId(
        bookingId = it.lastBooking().bookingId,
        offenderNo = it.nomsId,
        status = it.lastBooking().status(),
      )
    }

  fun findPrisonerDetails(bookingIds: List<Long>): List<PrisonerDetails> {
    return bookingRepository.findAllById(bookingIds)
      .map { PrisonerDetails(it.offender.nomsId, it.bookingId, it.location?.id ?: "") }
  }

  fun findPrisonerMerges(offenderNo: String, fromDate: LocalDate?): List<MergeDetail> {
    return mergeTransactionRepository.findByNomsIdAndAfterRequestDate(offenderNo, fromDate?.atStartOfDay())
      .map {
        val offender2Retained = offenderRepository.findLatestAliasByNomisId(it.nomsId2) != null
        MergeDetail(
          retainedOffenderNo = if (offender2Retained) it.nomsId2 else it.nomsId1,
          previousBookingId = it.offenderBookId1,
          deletedOffenderNo = if (offender2Retained) it.nomsId1 else it.nomsId2,
          activeBookingId = it.offenderBookId2,
          requestDateTime = it.requestDate,
        )
      }
  }

  fun Offender.lastBooking(): OffenderBooking =
    this.bookings.firstOrNull { it.bookingSequence == 1 } ?: throw IllegalStateException("Offender has no latest bookings")

  fun getPreviousBookingId(offenderNo: String, bookingId: Long): PreviousBookingId {
    val offender = offenderRepository.findRootByNomsId(offenderNo) ?: throw NotFoundException("Prisoner with offenderNo $offenderNo not found")

    return offender.bookings.firstOrNull { it.bookingId == bookingId }?.bookingSequence
      ?.let { bookingSequence -> offender.bookings.firstOrNull { it.bookingSequence == bookingSequence + 1 } }
      ?.let { PreviousBookingId(bookingId = it.bookingId, bookingSequence = it.bookingSequence!!.toLong()) }
      ?: throw NotFoundException("Prisoner with offenderNo $offenderNo and booking $bookingId not found or has no previous booking")
  }
}
