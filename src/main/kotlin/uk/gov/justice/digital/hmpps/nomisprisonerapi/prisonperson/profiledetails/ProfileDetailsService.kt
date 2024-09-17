package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.profiledetails

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.getReleaseTimer

@Service
@Transactional
class ProfileDetailsService(
  private val bookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
) {

  fun getProfileDetails(offenderNo: String): PrisonerProfileDetailsResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("No offender found for $offenderNo")
    }

    return bookingRepository.findAllByOffenderNomsId(offenderNo)
      .filterNot { it.profiles.isEmpty() }
      .map { booking ->
        BookingProfileDetailsResponse(
          bookingId = booking.bookingId,
          startDateTime = booking.bookingBeginDate,
          endDateTime = booking.getReleaseTimer(),
          latestBooking = booking.bookingSequence == 1,
          profileDetails = booking.profiles.first().profileDetails
            .map { pd ->
              ProfileDetailsResponse(
                type = pd.id.profileType.type,
                code = pd.profileCodeId,
                createDateTime = pd.createDatetime,
                createdBy = pd.createUserId,
                modifiedDateTime = pd.modifyDatetime,
                modifiedBy = pd.modifyUserId,
                auditModuleName = pd.auditModuleName,
              )
            },
        ).takeIf { bookingResponse -> bookingResponse.profileDetails.isNotEmpty() }
      }
      .filterNotNull()
      .let { PrisonerProfileDetailsResponse(offenderNo = offenderNo, bookings = it) }
  }
}
