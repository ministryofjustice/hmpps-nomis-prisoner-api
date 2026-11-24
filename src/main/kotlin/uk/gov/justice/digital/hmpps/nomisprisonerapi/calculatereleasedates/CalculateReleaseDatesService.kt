package uk.gov.justice.digital.hmpps.nomisprisonerapi.calculatereleasedates

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceCalculation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceCalculationRepository

@Service
@Transactional(readOnly = true)
class CalculateReleaseDatesService(
  private val offenderSentenceCalculationRepository: OffenderSentenceCalculationRepository,
) {
  fun getOffenderSentenceCalculation(offenderSentenceCalculationId: Long): OffenderSentenceCalculationResponse = offenderSentenceCalculationRepository.findByIdOrNull(offenderSentenceCalculationId)?.toResponse()
    ?: throw NotFoundException("Offender sentence calculation with id $offenderSentenceCalculationId not found")
}

private fun OffenderSentenceCalculation.toResponse() = OffenderSentenceCalculationResponse(
  id = id,
  bookingId = offenderBooking.bookingId,
  offenderNo = offenderBooking.offender.nomsId,
  calculationDate = calculationDate,
  staffId = staff?.id,
  calculationReasonCode = calculationReasonCode,
  commentText = commentText,
  effectiveSentenceEndDate = effectiveSentenceEndDate,
  effectiveSentenceLength = effectiveSentenceLength,
  judiciallyImposedSentenceLength = judiciallyImposedSentenceLength,
  workflowHistoryId = workflowHistoryId,
  hdcedCalculatedDate = hdcedCalculatedDate,
  hdcedOverridedDate = hdcedOverridedDate,
  hdcadCalculatedDate = hdcadCalculatedDate,
  hdcadOverridedDate = hdcadOverridedDate,
  etdCalculatedDate = etdCalculatedDate,
  etdOverridedDate = etdOverridedDate,
  mtdCalculatedDate = mtdCalculatedDate,
  mtdOverridedDate = mtdOverridedDate,
  ltdCalculatedDate = ltdCalculatedDate,
  ltdOverridedDate = ltdOverridedDate,
  ardCalculatedDate = ardCalculatedDate,
  ardOverridedDate = ardOverridedDate,
  crdCalculatedDate = crdCalculatedDate,
  crdOverridedDate = crdOverridedDate,
  pedCalculatedDate = pedCalculatedDate,
  pedOverridedDate = pedOverridedDate,
  apdCalculatedDate = apdCalculatedDate,
  apdOverridedDate = apdOverridedDate,
  npdCalculatedDate = npdCalculatedDate,
  npdOverridedDate = npdOverridedDate,
  ledCalculatedDate = ledCalculatedDate,
  ledOverridedDate = ledOverridedDate,
  sedCalculatedDate = sedCalculatedDate,
  sedOverridedDate = sedOverridedDate,
  prrdCalculatedDate = prrdCalculatedDate,
  prrdOverridedDate = prrdOverridedDate,
  tariffCalculatedDate = tariffCalculatedDate,
  tariffOverridedDate = tariffOverridedDate,
  dprrdCalculatedDate = dprrdCalculatedDate,
  dprrdOverridedDate = dprrdOverridedDate,
  ersedOverridedDate = ersedOverridedDate,
  tersedOverridedDate = tersedOverridedDate,
  rotlOverridedDate = rotlOverridedDate,
  tusedCalculatedDate = tusedCalculatedDate,
  tusedOverridedDate = tusedOverridedDate,
  recordDatetime = recordDatetime,
  recordUserId = recordUserId,
  createDatetime = createDatetime,
  createUsername = createUsername,
  modifyDatetime = modifyDatetime,
  modifyUserId = modifyUserId,
)
