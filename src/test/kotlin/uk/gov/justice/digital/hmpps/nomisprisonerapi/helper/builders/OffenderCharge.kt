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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderChargeRepository
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
  fun builder(): OffenderChargeBuilder = OffenderChargeBuilder(
    repository,
  )
}

@Component
class OffenderChargeBuilderRepository(
  val repository: OffenderChargeRepository,
  val chargeStatusTypeRepository: ReferenceCodeRepository<ChargeStatusType>,
  val pleaStatusTypeRepository: ReferenceCodeRepository<PleaStatusType>,
  val offenceResultCodeRepository: OffenceResultCodeRepository,
  val offenceRepository: OffenceRepository,
) {
  fun save(offenderCharge: OffenderCharge): OffenderCharge = repository.saveAndFlush(offenderCharge)

  fun lookupChargeStatus(code: String): ChargeStatusType = chargeStatusTypeRepository.findByIdOrNull(ChargeStatusType.pk(code))!!

  fun lookupPleaStatus(code: String): PleaStatusType = pleaStatusTypeRepository.findByIdOrNull(PleaStatusType.pk(code))!!

  fun lookupOffenceResultCode(code: String): OffenceResultCode = offenceResultCodeRepository.findByIdOrNull(code)!!

  fun lookupOffence(offenceCode: String, statuteCode: String): Offence = offenceRepository.findByIdOrNull(OffenceId(offenceCode = offenceCode, statuteCode = statuteCode))!!
}

class OffenderChargeBuilder(
  private val repository: OffenderChargeBuilderRepository,
) : OffenderChargeDsl {
  private lateinit var offenderCharge: OffenderCharge

  fun build(
    offenceCode: String,
    offencesCount: Int?,
    offenceDate: LocalDate?,
    offenceEndDate: LocalDate?,
    plea: String?,
    propertyValue: BigDecimal?,
    totalPropertyValue: BigDecimal?,
    cjitCode1: String?,
    cjitCode2: String?,
    cjitCode3: String?,
    resultCode1: String?,
    resultCode2: String?,
    mostSeriousFlag: Boolean,
    offenderBooking: OffenderBooking,
    courtCase: CourtCase,
    lidsOffenceNumber: Int?,
  ): OffenderCharge {
    val persistedResultCode1 = resultCode1?.let { repository.lookupOffenceResultCode(it) }
    val persistedResultCode2 = resultCode2?.let { repository.lookupOffenceResultCode(it) }
    return OffenderCharge(
      offenderBooking = offenderBooking,
      courtCase = courtCase,
      offence = repository.lookupOffence(offenceCode, offenceCode.take(4)),
      offencesCount = offencesCount,
      offenceDate = offenceDate,
      offenceEndDate = offenceEndDate,
      plea = plea?.let { repository.lookupPleaStatus(it) },
      propertyValue = propertyValue,
      totalPropertyValue = totalPropertyValue,
      cjitCode1 = cjitCode1,
      cjitCode2 = cjitCode2,
      cjitCode3 = cjitCode3,
      chargeStatus = persistedResultCode1?.let { repository.lookupChargeStatus(it.chargeStatus) },
      resultCode1 = persistedResultCode1,
      resultCode2 = persistedResultCode2,
      resultCode1Indicator = persistedResultCode1?.dispositionCode,
      resultCode2Indicator = persistedResultCode2?.dispositionCode,
      mostSeriousFlag = mostSeriousFlag,
      lidsOffenceNumber = lidsOffenceNumber,
    )
      .let { repository.save(it) }
      .also { offenderCharge = it }
  }
}
