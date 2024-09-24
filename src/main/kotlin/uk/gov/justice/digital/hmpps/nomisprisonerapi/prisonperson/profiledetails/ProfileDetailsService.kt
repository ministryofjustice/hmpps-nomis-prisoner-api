package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.profiledetails

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetailId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProfileCodeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderProfileRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProfileCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProfileTypeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.getReleaseTimer
import java.time.LocalDate

@Service
@Transactional
class ProfileDetailsService(
  private val bookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
  private val offenderProfileRepository: OffenderProfileRepository,
  private val profileTypeRepository: ProfileTypeRepository,
  private val profileCodeRepository: ProfileCodeRepository,
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

  fun upsertProfileDetails(offenderNo: String, request: UpsertProfileDetailsRequest): UpsertProfileDetailsResponse {
    // TODO SDIT-2023 Rewrite this once all tests written. Specifically make it more readable and lose all the !!
    var created = true
    val profileType = profileTypeRepository.findByIdOrNull(request.profileType)!!
    val profileCode = profileCodeRepository.findByIdOrNull(ProfileCodeId(request.profileType, request.profileCode!!))!!
    val booking = bookingRepository.findLatestByOffenderNomsId(offenderNo)
    val profile = booking!!.profiles.firstOrNull()
      ?: OffenderProfile(OffenderProfileId(booking, 1), LocalDate.now())
    profile.profileDetails.find { it.id.profileType.type == request.profileType }
      ?.apply { this.profileCodeId = profileCode.id.code }
      ?.also { created = false }
      ?: OffenderProfileDetail(OffenderProfileDetailId(booking, profile.id.sequence, profileType), 1L, profile, profileCode)
        .also { profile.profileDetails += it }
    offenderProfileRepository.save(profile)
    return UpsertProfileDetailsResponse(created, booking.bookingId)
  }
}
