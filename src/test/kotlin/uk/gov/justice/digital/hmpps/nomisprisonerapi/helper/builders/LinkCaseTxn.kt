package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEventChargeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LinkCaseTxn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LinkCaseTxnId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.LinkCaseTxnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@DslMarker
annotation class LinkCaseTxnDslMarker

@NomisDataDslMarker
interface LinkCaseTxnDsl

@Component
class LinkCaseTxnBuilderFactory(
  private val repository: LinkCaseTxnBuilderRepository,
) {
  fun builder(): LinkCaseTxnBuilder = LinkCaseTxnBuilder(
    repository,
  )
}

@Component
class LinkCaseTxnBuilderRepository(
  val repository: LinkCaseTxnRepository,
  val jdbcTemplate: JdbcTemplate,
  val caseStatusRepository: ReferenceCodeRepository<CaseStatus>,
) {
  fun get(sourceCase: CourtCase, targetCourtCase: CourtCase, offenderCharge: OffenderCharge): LinkCaseTxn = repository.findByIdOrNull(
    LinkCaseTxnId(
      caseId = sourceCase.id,
      combinedCaseId = targetCourtCase.id,
      offenderChargeId = offenderCharge.id,
    ),
  )!!
  fun insert(linkCaseTxn: LinkCaseTxn) {
    jdbcTemplate.update("insert into LINK_CASE_TXNS (CASE_ID, COMBINED_CASE_ID, OFFENDER_CHARGE_ID, EVENT_ID) values (?, ?, ?, ?) ", linkCaseTxn.id.caseId, linkCaseTxn.id.combinedCaseId, linkCaseTxn.id.offenderChargeId, linkCaseTxn.courtEvent.id)
  }
  fun updateCreateDatetime(linkCaseTxn: LinkCaseTxn, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update LINK_CASE_TXNS set CREATE_DATETIME = ? where LINK_CASE_TXNS.CASE_ID = ? and LINK_CASE_TXNS.COMBINED_CASE_ID = ? and LINK_CASE_TXNS.OFFENDER_CHARGE_ID = ?", whenCreated, linkCaseTxn.id.caseId, linkCaseTxn.id.combinedCaseId, linkCaseTxn.id.offenderChargeId)
  }
  fun lookupCaseStatus(code: String): CaseStatus = caseStatusRepository.findByIdOrNull(CaseStatus.pk(code))!!
}

class LinkCaseTxnBuilder(
  private val repository: LinkCaseTxnBuilderRepository,
) : LinkCaseTxnDsl {
  private lateinit var linkCaseTxn: LinkCaseTxn
  fun linkCases(
    sourceCase: CourtCase,
    targetCase: CourtCase,
    courtEvent: CourtEvent,
    whenCreated: LocalDateTime,
  ): List<LinkCaseTxn> {
    val chargesToCopy = sourceCase.offenderCharges.filterNot { it.resultCode1?.dispositionCode == "F" }
    chargesToCopy.forEach { it.courtCase = targetCase }
    sourceCase.offenderCharges.removeAll(chargesToCopy)
    sourceCase.targetCombinedCase = targetCase
    sourceCase.caseStatus = repository.lookupCaseStatus("I")
    targetCase.offenderCharges += chargesToCopy

    courtEvent.courtEventCharges += chargesToCopy.map {
      CourtEventCharge(
        id = CourtEventChargeId(
          offenderCharge = it,
          courtEvent = courtEvent,
        ),
        offencesCount = it.offencesCount,
        offenceDate = it.offenceDate,
        offenceEndDate = it.offenceEndDate,
        plea = it.plea,
        propertyValue = it.propertyValue,
        totalPropertyValue = it.totalPropertyValue,
        cjitCode1 = it.cjitCode1,
        cjitCode2 = it.cjitCode2,
        cjitCode3 = it.cjitCode3,
        resultCode1 = it.resultCode1,
        resultCode2 = it.resultCode2,
        resultCode1Indicator = it.resultCode1Indicator,
        resultCode2Indicator = it.resultCode2Indicator,
        mostSeriousFlag = it.mostSeriousFlag,
      )
    }
    return chargesToCopy.map {
      build(
        sourceCase = sourceCase,
        targetCase = targetCase,
        courtEvent = courtEvent,
        offenderCharge = it,
        whenCreated = whenCreated,
      )
    }
  }

  fun build(
    sourceCase: CourtCase,
    targetCase: CourtCase,
    courtEvent: CourtEvent,
    offenderCharge: OffenderCharge,
    whenCreated: LocalDateTime?,
  ): LinkCaseTxn = LinkCaseTxn(
    id = LinkCaseTxnId(
      caseId = sourceCase.id,
      combinedCaseId = targetCase.id,
      offenderChargeId = offenderCharge.id,
    ),
    sourceCase = sourceCase,
    targetCase = targetCase,
    offenderCharge = offenderCharge,
    courtEventCharge = courtEvent.courtEventCharges.find { it.id.offenderCharge == offenderCharge }!!,
    courtEvent = courtEvent,
  )
    .let {
      // cannot get JPA to work without throwing detached entity exceptions
      // so insert using SQL but then load using JPA
      repository.insert(it)
      repository.get(sourceCase, targetCase, offenderCharge)
    }
    .also {
      if (whenCreated != null) {
        repository.updateCreateDatetime(it, whenCreated)
      }
    }
    .also { linkCaseTxn = it }
}
