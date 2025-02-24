package uk.gov.justice.digital.hmpps.nomisprisonerapi.profiledetails

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetailId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProfileCodeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProfileTypeCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProfileCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProfileTypeRepository
import java.time.LocalDate

@Service
@Transactional
class ProfileDetailsService(
  private val bookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
  private val profileTypeRepository: ProfileTypeRepository,
  private val profileCodeRepository: ProfileCodeRepository,
  private val telemetryClient: TelemetryClient,
) {

  fun getProfileDetails(offenderNo: String): PrisonerProfileDetailsResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("No offender found for $offenderNo")
    }

    return bookingRepository.findAllByOffenderNomsId(offenderNo)
      .mapNotNull { booking ->
        BookingProfileDetailsResponse(
          bookingId = booking.bookingId,
          latestBooking = booking.bookingSequence == 1,
          profileDetails = booking.profileDetails
            .filter { it.id.sequence == 1L }
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
      .let { PrisonerProfileDetailsResponse(offenderNo = offenderNo, bookings = it) }
  }

  fun upsertProfileDetails(offenderNo: String, request: UpsertProfileDetailsRequest): UpsertProfileDetailsResponse {
    validateProfileType(request.profileType)
    validateProfileCode(request.profileType, request.profileCode)

    val booking = findLatestBookingOrThrow(offenderNo)
    val (profileDetails, created) =
      booking
        .also { it.getOrCreateProfile() }
        .getOrCreateProfileDetails(request)
    profileDetails.profileCodeId = request.profileCode

    return UpsertProfileDetailsResponse(created, booking.bookingId)
      .also {
        telemetryClient.trackEvent(
          """physical-attributes-profile-details-${if (created) "created" else "updated"}""",
          mapOf(
            "offenderNo" to offenderNo,
            "bookingId" to booking.bookingId.toString(),
            "profileType" to request.profileType,
          ),
          null,
        )
      }
  }

  private fun OffenderBooking.getOrCreateProfileDetails(request: UpsertProfileDetailsRequest) = profileDetails
    .find { it.id.profileType.type == request.profileType }
    ?.let { it to false }
    ?: (createNewProfileDetails(request) to true)

  private fun OffenderBooking.createNewProfileDetails(request: UpsertProfileDetailsRequest) = OffenderProfileDetail(
    id = OffenderProfileDetailId(
      offenderBooking = this,
      sequence = 1L,
      profileType = getProfileType(request.profileType),
    ),
    listSequence = 1L,
    profileCodeId = request.profileCode,
  )
    .also { profileDetails += it }

  private fun OffenderBooking.getOrCreateProfile() = profiles.firstOrNull { it.id.sequence == 1L }
    ?: OffenderProfile(OffenderProfileId(this, 1L), LocalDate.now())
      .also { profiles += it }

  private fun validateProfileType(profileType: String) {
    if (!profileTypeRepository.existsById(profileType)) {
      throw BadDataException("Invalid profile type $profileType")
    }
  }

  private fun getProfileType(profileType: String) = profileTypeRepository.findByIdOrNull(profileType)
    ?: throw BadDataException("Invalid profile type $profileType")

  private fun validateProfileCode(profileType: String, profileCode: String?) {
    profileCode
      ?.takeIf { getProfileType(profileType) is ProfileTypeCode }
      ?.also {
        if (!profileCodeRepository.existsById(ProfileCodeId(profileType, profileCode))) {
          throw BadDataException("Invalid profile code $profileCode for profile type $profileType")
        }
      }
  }

  private fun findLatestBookingOrThrow(offenderNo: String) = bookingRepository.findLatestByOffenderNomsId(offenderNo)
    ?: throw NotFoundException("No latest booking found for $offenderNo")
}
