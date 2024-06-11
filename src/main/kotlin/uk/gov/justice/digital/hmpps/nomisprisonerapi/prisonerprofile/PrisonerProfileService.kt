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
  private val repository: OffenderBookingRepository,
) {
  fun getPhysicalAttributes(offenderNo: String) =
    repository.findAllByOffenderNomsId(offenderNo)
      .takeIf { it.isNotEmpty() }
      ?.map {
        BookingPhysicalAttributesResponse(
          bookingId = it.bookingId,
          startDate = it.bookingBeginDate.toLocalDate(),
          endDate = it.bookingEndDate?.toLocalDate(),
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
