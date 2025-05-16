package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LinkCaseTxn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LinkCaseTxnId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.LinkCaseTxnRepository
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
) {
  fun save(linkCaseTxn: LinkCaseTxn): LinkCaseTxn = repository.saveAndFlush(linkCaseTxn)
  fun updateCreateDatetime(linkCaseTxn: LinkCaseTxn, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update LINK_CASE_TXNS set CREATE_DATETIME = ? where LINK_CASE_TXNS.CASE_ID = ? and LINK_CASE_TXNS.COMBINED_CASE_ID = ? and LINK_CASE_TXNS.OFFENDER_CHARGE_ID = ?", whenCreated, linkCaseTxn.id.caseId, linkCaseTxn.id.combinedCaseId, linkCaseTxn.id.offenderChargeId)
  }
}

class LinkCaseTxnBuilder(
  private val repository: LinkCaseTxnBuilderRepository,
) : LinkCaseTxnDsl {
  private lateinit var linkCaseTxn: LinkCaseTxn

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
    .let { repository.save(it) }
    .also {
      if (whenCreated != null) {
        repository.updateCreateDatetime(it, whenCreated)
      }
    }
    .also { linkCaseTxn = it }
}
