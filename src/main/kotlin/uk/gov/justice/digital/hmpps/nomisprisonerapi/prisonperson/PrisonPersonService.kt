package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.BookingPhysicalAttributesResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.PhysicalAttributesResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.PrisonerPhysicalAttributesResponse
import java.time.LocalDateTime

@Service
@Transactional
class PrisonPersonService(
  private val bookingRepository: OffenderBookingRepository,
) {
  fun getPhysicalAttributes(offenderNo: String) =
    bookingRepository.findAllByOffenderNomsId(offenderNo)
      .takeIf { it.isNotEmpty() }
      ?.filterNot { it.physicalAttributes.isEmpty() }
      ?.map {
        BookingPhysicalAttributesResponse(
          bookingId = it.bookingId,
          startDateTime = it.bookingBeginDate,
          endDateTime = it.getReleaseTime(),
          latestBooking = it.bookingSequence == 1,
          physicalAttributes = it.physicalAttributes.map {
            PhysicalAttributesResponse(
              attributeSequence = it.id.sequence,
              heightCentimetres = it.getHeightInCentimetres(),
              weightKilograms = it.getWeightInKilograms(),
              createDateTime = it.createDatetime,
              createdBy = it.createUserId,
              modifiedDateTime = it.modifyDatetime,
              modifiedBy = it.modifyUserId,
              auditModuleName = it.auditModuleName,
            )
          },
        )
      }?.let {
        PrisonerPhysicalAttributesResponse(offenderNo = offenderNo, bookings = it)
      }
      ?: throw NotFoundException("No bookings found for offender $offenderNo")

  // NOMIS truncates the time from booking end date, so try and get the accurate time from the last release movement
  private fun OffenderBooking.getReleaseTime(): LocalDateTime? =
    bookingEndDate?.let {
      externalMovements
        .filter { it.movementType.code == "REL" }
        .maxByOrNull { it.movementTime }
        ?.movementTime
        ?: bookingEndDate
    }
}
