package uk.gov.justice.digital.hmpps.nomisprisonerapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.IncentiveIdResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.IncentiveResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.filter.IncentiveFilter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncentiveId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncentiveRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.IncentiveSpecification
import java.time.LocalDateTime

@Service
@Transactional
class IncentivesService(
  private val incentiveRepository: IncentiveRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val offenderBookingRepository: OffenderBookingRepository
) {
  fun findIncentiveIdsByFilter(pageRequest: Pageable, incentiveFilter: IncentiveFilter): Page<IncentiveIdResponse> {
    return incentiveRepository.findAll(IncentiveSpecification(incentiveFilter), pageRequest)
      .map { IncentiveIdResponse(bookingId = it.id.offenderBooking.bookingId, sequence = it.id.sequence) }
  }

  fun getIncentive(bookingId: Long, incentiveSequence: Long): IncentiveResponse {
    val offenderBooking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Offender booking $bookingId not found")
    return incentiveRepository.findByIdOrNull(
      IncentiveId(
        offenderBooking = offenderBooking,
        sequence = incentiveSequence
      )
    )?.let { mapIncentiveModel(it) }
      ?: throw NotFoundException("Incentive not found, booking id $bookingId, sequence $incentiveSequence")
  }

  private fun mapIncentiveModel(incentiveEntity: Incentive): IncentiveResponse {

    val prison = agencyLocationRepository.findById(incentiveEntity.location.id)
      .orElseThrow { NotFoundException("prison Id ${incentiveEntity.location.id} not found") }

    return IncentiveResponse(
      bookingId = incentiveEntity.id.offenderBooking.bookingId,
      incentiveSequence = incentiveEntity.id.sequence,
      commentText = incentiveEntity.commentText,
      iepDateTime = LocalDateTime.of(incentiveEntity.iepDate, incentiveEntity.iepTime),
      iepLevel = CodeDescription(incentiveEntity.iepLevel.code, incentiveEntity.iepLevel.description),
      prisonId = prison.id
    )
  }
}
