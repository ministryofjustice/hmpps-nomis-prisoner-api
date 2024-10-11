package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.reconciliation

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProfileDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.physicalattributes.getHeightInCentimetres
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.physicalattributes.getWeightInKilograms

@Service
@Transactional
class PrisonPersonReconService(
  private val bookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
) {

  fun getReconciliation(offenderNo: String): PrisonPersonReconciliationResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("No offender found for $offenderNo")
    }

    val latestBooking = bookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("No bookings found for $offenderNo")

    val physicalAttributes = latestBooking.physicalAttributes.minByOrNull { it.id.sequence }
    val profileDetails = latestBooking.profileDetails.filter { it.id.sequence == 1L }

    return PrisonPersonReconciliationResponse(
      offenderNo = offenderNo,
      height = physicalAttributes?.getHeightInCentimetres(),
      weight = physicalAttributes?.getWeightInKilograms(),
      face = profileDetails.findProfileCode("FACE"),
      build = profileDetails.findProfileCode("BUILD"),
      facialHair = profileDetails.findProfileCode("FACIAL_HAIR"),
      hair = profileDetails.findProfileCode("HAIR"),
      leftEyeColour = profileDetails.findProfileCode("L_EYE_C"),
      rightEyeColour = profileDetails.findProfileCode("R_EYE_C"),
      shoeSize = profileDetails.findProfileCode("SHOESIZE"),
    )
  }

  private fun List<OffenderProfileDetail>.findProfileCode(profileType: String) =
    find { it.id.profileType.type == profileType }?.profileCodeId
}
