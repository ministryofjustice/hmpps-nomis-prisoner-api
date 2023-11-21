package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ChargeStatusType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenceId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenceResultCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PleaStatusType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceResultCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate

@DslMarker
annotation class OffenderChargeDslMarker

@NomisDataDslMarker
interface OffenderChargeDsl

@Component
class OffenderChargeBuilderFactory(
  private val repository: OffenderChargeBuilderRepository,
) {
  fun builder(): OffenderChargeBuilder {
    return OffenderChargeBuilder(
      repository,
    )
  }
}

@Component
class OffenderChargeBuilderRepository(
  val chargeStatusTypeRepository: ReferenceCodeRepository<ChargeStatusType>,
  val pleaStatusTypeRepository: ReferenceCodeRepository<PleaStatusType>,
  val offenceResultCodeRepository: OffenceResultCodeRepository,
  val offenceRepository: OffenceRepository,
) {
  fun lookupChargeStatus(code: String): ChargeStatusType =
    chargeStatusTypeRepository.findByIdOrNull(ChargeStatusType.pk(code))!!

  fun lookupPleaStatus(code: String): PleaStatusType =
    pleaStatusTypeRepository.findByIdOrNull(PleaStatusType.pk(code))!!

  fun lookupOffenceResultCode(code: String): OffenceResultCode = offenceResultCodeRepository.findByIdOrNull(code)!!

  fun lookupOffence(offenceCode: String, statuteCode: String): Offence =
    offenceRepository.findByIdOrNull(OffenceId(offenceCode = offenceCode, statuteCode = statuteCode))!!
}

class OffenderChargeBuilder(
  private val repository: OffenderChargeBuilderRepository,
) : OffenderChargeDsl {
  private lateinit var offenderCharge: OffenderCharge

  fun build(
    offenceCode: String,
    statuteCode: String,
    offencesCount: Int?,
    offenceDate: LocalDate?,
    offenceEndDate: LocalDate?,
    plea: String?,
    propertyValue: BigDecimal?,
    totalPropertyValue: BigDecimal?,
    cjitCode1: String?,
    cjitCode2: String?,
    cjitCode3: String?,
    chargeStatus: String?,
    resultCode1: String?,
    resultCode2: String?,
    resultCode1Indicator: String?,
    resultCode2Indicator: String?,
    mostSeriousFlag: Boolean,
    offenderBooking: OffenderBooking,
    courtCase: CourtCase,
    lidsOffenceNumber: Int?,
  ): OffenderCharge = OffenderCharge(
    offenderBooking = offenderBooking,
    courtCase = courtCase,
    offence = repository.lookupOffence(offenceCode, statuteCode),
    offencesCount = offencesCount,
    offenceDate = offenceDate,
    offenceEndDate = offenceEndDate,
    plea = plea?. let { repository.lookupPleaStatus(it) },
    propertyValue = propertyValue,
    totalPropertyValue = totalPropertyValue,
    cjitCode1 = cjitCode1,
    cjitCode2 = cjitCode2,
    cjitCode3 = cjitCode3,
    chargeStatus = chargeStatus?. let { repository.lookupChargeStatus(it) },
    resultCode1 = resultCode1?. let { repository.lookupOffenceResultCode(it) },
    resultCode2 = resultCode2?. let { repository.lookupOffenceResultCode(it) },
    resultCode1Indicator = resultCode1Indicator,
    resultCode2Indicator = resultCode2Indicator,
    mostSeriousFlag = mostSeriousFlag,
    lidsOffenceNumber = lidsOffenceNumber,
  )
    .also { offenderCharge = it }
}
