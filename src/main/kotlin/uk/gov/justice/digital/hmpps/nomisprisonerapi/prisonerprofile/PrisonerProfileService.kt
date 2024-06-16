package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile.api.BookingPhysicalAttributesResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile.api.PhysicalAttributesResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile.api.PrisonerPhysicalAttributesResponse

@Service
@Transactional
class PrisonerProfileService(
  private val bookingRepository: OffenderBookingRepository,
) {
  fun getPhysicalAttributes(offenderNo: String) =
    bookingRepository.findAllByOffenderNomsId(offenderNo)
      .takeIf { it.isNotEmpty() }
      ?.filterNot { it.physicalAttributes.isEmpty() }
      ?.map {
        BookingPhysicalAttributesResponse(
          bookingId = it.bookingId,
          startDate = it.bookingBeginDate.toLocalDate(),
          endDate = it.bookingEndDate?.toLocalDate(),
          latestBooking = it.bookingSequence == 1,
          physicalAttributes = it.physicalAttributes.map {
            PhysicalAttributesResponse(
              heightCentimetres = it.getHeightInCentimetres(),
              weightKilograms = it.getWeightInKilograms(),
              createDateTime = it.createDatetime,
              modifiedDateTime = it.modifyDatetime,
              auditModuleName = it.auditModuleName,
            )
          },
        )
      }?.let {
        PrisonerPhysicalAttributesResponse(offenderNo = offenderNo, bookings = it)
      }
      ?: throw NotFoundException("No bookings found for offender $offenderNo")
}
