package uk.gov.justice.digital.hmpps.nomisprisonerapi.bedassignments

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BedAssignmentHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BedAssignmentHistoryId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.BedAssignmentHistoryRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository

@Service
@Transactional(readOnly = true)
class BedAssignmentHistoryService(
  private val bedAssignmentHistoryRepository: BedAssignmentHistoryRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
) {
  fun getBedAssignment(bookingId: Long, sequence: Int): BedAssignmentHistoryResponse {
    val booking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw NotFoundException("Booking with id $bookingId not found")

    return bedAssignmentHistoryRepository.findByIdOrNull(BedAssignmentHistoryId(booking, sequence))
      ?.toResponse()
      ?: throw NotFoundException("Bed assignment for booking $bookingId and sequence $sequence not found")
  }

  fun getBedAssignments(offenderNo: String): PrisonerBedAssignmentHistoryResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("Prisoner $offenderNo not found")
    }
    return PrisonerBedAssignmentHistoryResponse(
      bedAssignmentHistoryRepository.findAllForPrisoner(offenderNo).map { it.toResponse() },
    )
  }
}

private fun BedAssignmentHistory.toResponse() = BedAssignmentHistoryResponse(
  bookingId = id.offenderBooking.bookingId,
  sequence = id.sequence,
  livingUnitId = livingUnitId,
  livingUnitDescription = livingUnit?.description,
  prisonId = livingUnit?.agency?.id,
  assignmentDateTime = assignmentDate.atTime(assignmentTime.toLocalTime()),
  assignmentEndDateTime = assignmentEndDate?.let { date -> assignmentEndTime?.let { date.atTime(it.toLocalTime()) } ?: date.atStartOfDay() },
  assignmentReasonCode = assignmentReasonCode,
  assignmentReasonDescription = assignmentReason?.description,
  audit = toAudit(),
)
