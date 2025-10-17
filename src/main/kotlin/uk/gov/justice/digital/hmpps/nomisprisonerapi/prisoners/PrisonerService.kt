package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.MergeTransactionRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.findLatestAliasByNomisId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.ActiveBookingsSpecification
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class PrisonerService(
  private val bookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
  private val mergeTransactionRepository: MergeTransactionRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
) {
  fun findAllActivePrisoners(pageRequest: Pageable): Page<PrisonerIds> = bookingRepository.findAll(ActiveBookingsSpecification(), pageRequest)
    .map { PrisonerIds(bookingId = it.bookingId, offenderNo = it.offender.nomsId) }

  fun findAllPrisonersWithBookings(pageRequest: Pageable): Page<PrisonerIds> = offenderRepository.findAllWithBookings(
    PageRequest.of(
      pageRequest.pageNumber,
      pageRequest.pageSize,
      Sort.by(ASC, "o.rootOffenderId"),
    ),
  ).map {
    PrisonerIds(
      bookingId = it.getBookingId(),
      offenderNo = it.getNomsId(),
    )
  }

  fun findAllPrisoners(pageRequest: Pageable): Page<PrisonerId> = offenderRepository.findAllIds(pageRequest).map { PrisonerId(offenderNo = it.getNomsId()) }

  fun findAllPrisonersFromId(offenderId: Long, pageSize: Int): PrisonerNosWithLast = offenderRepository.findAllIdsFromId(offenderId, pageSize).let {
    PrisonerNosWithLast(
      it.map { PrisonerId(it.getPrisonerId()) },
      it.lastOrNull()?.getOffenderId() ?: 0,
    )
  }

  fun findPrisonerDetails(bookingIds: List<Long>): List<PrisonerDetails> = bookingRepository.findAllById(bookingIds)
    .map { it.toPrisonerDetails() }

  fun findPrisonerDetails(offenderNo: String): PrisonerDetails = bookingRepository.findLatestByOffenderNomsId(offenderNo)?.toPrisonerDetails()
    ?: throw NotFoundException("No prisoner with offender $offenderNo found")

  fun findPrisonerMerges(offenderNo: String, fromDate: LocalDate?): List<MergeDetail> = mergeTransactionRepository.findByNomsIdAndAfterRequestDate(offenderNo, fromDate?.atStartOfDay())
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

  fun getPreviousBookingId(offenderNo: String, bookingId: Long): PreviousBookingId = offenderBookingRepository.findByIdOrNull(bookingId)
    ?.bookingSequence
    ?.let { bookingSequence ->
      offenderBookingRepository.findOneByOffenderNomsIdAndBookingSequence(
        offenderNo,
        bookingSequence + 1,
      )
    }
    ?.let { PreviousBookingId(bookingId = it.bookingId, bookingSequence = it.bookingSequence.toLong()) }
    ?: throw NotFoundException("Prisoner with offenderNo $offenderNo and booking $bookingId not found or has no previous booking")
}

private fun OffenderBooking.toPrisonerDetails(): PrisonerDetails = PrisonerDetails(
  offenderNo = offender.nomsId,
  offenderId = offender.id,
  bookingId = bookingId,
  location = location.id,
  active = active,
)

data class PrisonerNosWithLast(
  val prisonerIds: List<PrisonerId>,
  val lastOffenderId: Long,
)
