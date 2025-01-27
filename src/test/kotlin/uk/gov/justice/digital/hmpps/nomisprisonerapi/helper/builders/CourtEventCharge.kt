package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventChargeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenceResultCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PleaStatusType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventChargeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenceResultCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class CourtEventChargeDslMarker

@NomisDataDslMarker
interface CourtEventChargeDsl

@Component
class CourtEventChargeBuilderFactory(
  private val repository: CourtEventChargeBuilderRepository,
) {
  fun builder(): CourtEventChargeBuilder = CourtEventChargeBuilder(
    repository,
  )
}

@Component
class CourtEventChargeBuilderRepository(
  val pleaStatusTypeRepository: ReferenceCodeRepository<PleaStatusType>,
  val offenceResultCodeRepository: OffenceResultCodeRepository,
  val courtEventChargeRepository: CourtEventChargeRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(cec: CourtEventCharge): CourtEventCharge = courtEventChargeRepository.saveAndFlush(cec)
  fun updateModifiedDatetime(cec: CourtEventCharge, whenModified: LocalDateTime) {
    jdbcTemplate.update(
      "update COURT_EVENT_CHARGES set MODIFY_DATETIME = ? where OFFENDER_CHARGE_ID = ? and EVENT_ID = ?",
      whenModified,
      cec.id.offenderCharge.id,
      cec.id.courtEvent.id,
    )
  }

  fun lookupPleaStatus(code: String): PleaStatusType = pleaStatusTypeRepository.findByIdOrNull(PleaStatusType.pk(code))!!

  fun lookupOffenceResultCode(code: String): OffenceResultCode = offenceResultCodeRepository.findByIdOrNull(code)!!
}

class CourtEventChargeBuilder(
  private val repository: CourtEventChargeBuilderRepository,
) : CourtEventChargeDsl {
  private lateinit var courtEventCharge: CourtEventCharge

  fun build(
    courtEvent: CourtEvent,
    offenderCharge: OffenderCharge,
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
    resultCode1Indicator: String?,
    resultCode2Indicator: String?,
    mostSeriousFlag: Boolean,
    whenModified: LocalDateTime?,
  ): CourtEventCharge = CourtEventCharge(
    id = CourtEventChargeId(courtEvent = courtEvent, offenderCharge = offenderCharge),
    offencesCount = offencesCount,
    offenceDate = offenceDate,
    offenceEndDate = offenceEndDate,
    plea = plea?.let { repository.lookupPleaStatus(it) },
    propertyValue = propertyValue,
    totalPropertyValue = totalPropertyValue,
    cjitCode1 = cjitCode1,
    cjitCode2 = cjitCode2,
    cjitCode3 = cjitCode3,
    resultCode1 = resultCode1?.let { repository.lookupOffenceResultCode(it) },
    resultCode2 = resultCode2?.let { repository.lookupOffenceResultCode(it) },
    resultCode1Indicator = resultCode1Indicator,
    resultCode2Indicator = resultCode2Indicator,
    mostSeriousFlag = mostSeriousFlag,
  ).let { repository.save(it) }
    .also {
      if (whenModified != null) {
        repository.updateModifiedDatetime(it, whenModified)
      }
      courtEventCharge = it
    }
}
