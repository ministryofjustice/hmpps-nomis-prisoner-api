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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncentiveRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.IncentiveSpecification
import java.time.LocalDateTime

@Service
@Transactional
class IncentivesService(
  private val incentiveRepository: IncentiveRepository,
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
    )?.let {
      // determine if this incentive is the current IEP for the booking
      val currentIep =
        it == incentiveRepository.findFirstById_offenderBookingOrderByIepDateDescId_SequenceDesc(offenderBooking)
      mapIncentiveModel(it, currentIep)
    }
      ?: throw NotFoundException("Incentive not found, booking id $bookingId, sequence $incentiveSequence")
  }

  fun getCurrentIncentive(bookingId: Long): IncentiveResponse {
    val offenderBooking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Offender booking $bookingId not found")
    return incentiveRepository.findFirstById_offenderBookingOrderByIepDateDescId_SequenceDesc(offenderBooking)
      ?.let {
        mapIncentiveModel(incentiveEntity = it, currentIep = true)
      } ?: throw NotFoundException("Current Incentive not found, booking id $bookingId")
  }

  private fun mapIncentiveModel(incentiveEntity: Incentive, currentIep: Boolean): IncentiveResponse {
    return IncentiveResponse(
      bookingId = incentiveEntity.id.offenderBooking.bookingId,
      incentiveSequence = incentiveEntity.id.sequence,
      commentText = incentiveEntity.commentText,
      iepDateTime = LocalDateTime.of(incentiveEntity.iepDate, incentiveEntity.iepTime),
      iepLevel = CodeDescription(incentiveEntity.iepLevel.code, incentiveEntity.iepLevel.description),
      prisonId = incentiveEntity.location.id,
      userId = incentiveEntity.userId,
      currentIep = currentIep,
      offenderNo = incentiveEntity.id.offenderBooking.offender.nomsId,
      auditModule = incentiveEntity.auditModuleName,
    )
  }
}
