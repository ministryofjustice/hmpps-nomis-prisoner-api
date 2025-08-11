package uk.gov.justice.digital.hmpps.nomisprisonerapi.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LinkCaseTxn

@Repository
class LinkCaseTxnRepositorySql(
  val jdbcTemplate: JdbcTemplate,
) {
  fun insert(linkCaseTxn: LinkCaseTxn) {
    jdbcTemplate.update("insert into LINK_CASE_TXNS (CASE_ID, COMBINED_CASE_ID, OFFENDER_CHARGE_ID, EVENT_ID) values (?, ?, ?, ?) ", linkCaseTxn.id.caseId, linkCaseTxn.id.combinedCaseId, linkCaseTxn.id.offenderChargeId, linkCaseTxn.courtEvent.id)
  }
}
