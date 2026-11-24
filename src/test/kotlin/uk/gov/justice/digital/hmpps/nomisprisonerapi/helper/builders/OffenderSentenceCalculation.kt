package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceCalculation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceCalculationRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderSentenceCalculationDslMarker

@OffenderSentenceCalculationDslMarker
interface OffenderSentenceCalculationDsl

@Component
class OffenderSentenceCalculationBuilderFactory(
  private val repository: OffenderSentenceCalculationBuilderRepository,
) {
  fun builder(): OffenderSentenceCalculationBuilder = OffenderSentenceCalculationBuilder(repository)
}

@Component
class OffenderSentenceCalculationBuilderRepository(
  val offenderSentenceCalculationRepository: OffenderSentenceCalculationRepository,
) {
  fun save(calculation: OffenderSentenceCalculation): OffenderSentenceCalculation = offenderSentenceCalculationRepository.saveAndFlush(calculation)
}

class OffenderSentenceCalculationBuilder(
  private val repository: OffenderSentenceCalculationBuilderRepository,
) : OffenderSentenceCalculationDsl {
  fun build(
    calculationDate: LocalDateTime,
    staff: Staff?,
    calculationReasonCode: String,
    commentText: String?,
    effectiveSentenceEndDate: LocalDate?,
    effectiveSentenceLength: String?,
    judiciallyImposedSentenceLength: String?,
    workflowHistoryId: Long?,
    recordDatetime: LocalDateTime,
    recordUserId: String,
    hdcedCalculatedDate: LocalDate?,
    hdcedOverridedDate: LocalDate?,
    hdcadCalculatedDate: LocalDate?,
    hdcadOverridedDate: LocalDate?,
    etdCalculatedDate: LocalDate?,
    etdOverridedDate: LocalDate?,
    mtdCalculatedDate: LocalDate?,
    mtdOverridedDate: LocalDate?,
    ltdCalculatedDate: LocalDate?,
    ltdOverridedDate: LocalDate?,
    ardCalculatedDate: LocalDate?,
    ardOverridedDate: LocalDate?,
    crdCalculatedDate: LocalDate?,
    crdOverridedDate: LocalDate?,
    pedCalculatedDate: LocalDate?,
    pedOverridedDate: LocalDate?,
    apdCalculatedDate: LocalDate?,
    apdOverridedDate: LocalDate?,
    npdCalculatedDate: LocalDate?,
    npdOverridedDate: LocalDate?,
    ledCalculatedDate: LocalDate?,
    ledOverridedDate: LocalDate?,
    sedCalculatedDate: LocalDate?,
    sedOverridedDate: LocalDate?,
    prrdCalculatedDate: LocalDate?,
    prrdOverridedDate: LocalDate?,
    tariffCalculatedDate: LocalDate?,
    tariffOverridedDate: LocalDate?,
    dprrdCalculatedDate: LocalDate?,
    dprrdOverridedDate: LocalDate?,
    tusedCalculatedDate: LocalDate?,
    tusedOverridedDate: LocalDate?,
    ersedOverridedDate: LocalDate?,
    tersedOverridedDate: LocalDate?,
    rotlOverridedDate: LocalDate?,
    offenderBooking: OffenderBooking,
  ): OffenderSentenceCalculation = OffenderSentenceCalculation(
    offenderBooking = offenderBooking,
    calculationDate = calculationDate,
    staff = staff,
    calculationReasonCode = calculationReasonCode,
    commentText = commentText,
    effectiveSentenceEndDate = effectiveSentenceEndDate,
    effectiveSentenceLength = effectiveSentenceLength,
    judiciallyImposedSentenceLength = judiciallyImposedSentenceLength,
    workflowHistoryId = workflowHistoryId,
    recordDatetime = recordDatetime,
    recordUserId = recordUserId,
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
    tusedCalculatedDate = tusedCalculatedDate,
    tusedOverridedDate = tusedOverridedDate,
    ersedOverridedDate = ersedOverridedDate,
    tersedOverridedDate = tersedOverridedDate,
    rotlOverridedDate = rotlOverridedDate,
  ).let { repository.save(it) }
}
