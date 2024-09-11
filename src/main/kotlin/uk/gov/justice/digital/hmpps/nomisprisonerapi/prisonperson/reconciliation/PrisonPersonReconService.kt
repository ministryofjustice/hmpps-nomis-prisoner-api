package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.reconciliation

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
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

    return latestBooking.physicalAttributes.minByOrNull { it.id.sequence }
      ?.let {
        PrisonPersonReconciliationResponse(
          offenderNo = offenderNo,
          height = it.getHeightInCentimetres(),
          weight = it.getWeightInKilograms(),
        )
      }
      ?: PrisonPersonReconciliationResponse(offenderNo = offenderNo)
  }
}
