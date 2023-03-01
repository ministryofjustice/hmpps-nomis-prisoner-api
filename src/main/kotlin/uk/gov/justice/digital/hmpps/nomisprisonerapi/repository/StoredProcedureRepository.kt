package uk.gov.justice.digital.hmpps.nomisprisonerapi.repository

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.AuditProcedure
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.ClearAuditProcedure
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.KeyDateAdjustmentDelete
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.KeyDateAdjustmentUpsert

interface StoredProcedureRepository {
  fun postKeyDateAdjustmentUpsert(
    keyDateAdjustmentId: Long,
    bookingId: Long,
  )

  fun preKeyDateAdjustmentDeletion(
    keyDateAdjustmentId: Long,
    bookingId: Long,
  )

  fun audit(name: String)
  fun clearAudit()
}

@Repository
@Profile("oracle")
class StoredProcedureRepositoryOracle(
  private val keyDateAdjustmentUpsertProcedure: KeyDateAdjustmentUpsert,
  private val keyDateAdjustmentDeleteProcedure: KeyDateAdjustmentDelete,
  private val auditProcedure: AuditProcedure,
  private val clearAuditProcedure: ClearAuditProcedure,
) :
  StoredProcedureRepository {

  override fun postKeyDateAdjustmentUpsert(
    keyDateAdjustmentId: Long,
    bookingId: Long,
  ) {
    val params = MapSqlParameterSource()
      .addValue("p_offbook_id", bookingId)
      .addValue("p_key_date_id", keyDateAdjustmentId)
    keyDateAdjustmentUpsertProcedure.execute(params)
  }

  override fun preKeyDateAdjustmentDeletion(
    keyDateAdjustmentId: Long,
    bookingId: Long,
  ) {
    val params = MapSqlParameterSource()
      .addValue("p_offender_book_id", bookingId)
      .addValue("p_offender_key_date_adjust_id", keyDateAdjustmentId)
    keyDateAdjustmentDeleteProcedure.execute(params)
  }

  override fun audit(name: String) {
    val paramMap: SqlParameterSource = MapSqlParameterSource()
      .addValue("V_NAME", "AUDIT_MODULE_NAME")
      .addValue("V_VALUE", name)

    auditProcedure.execute(paramMap)
  }

  override fun clearAudit() {
    clearAuditProcedure.execute()
  }
}

@Repository
@Profile("!oracle")
class StoredProcedureRepositoryH2() : StoredProcedureRepository {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun postKeyDateAdjustmentUpsert(
    keyDateAdjustmentId: Long,
    bookingId: Long,
  ) {
    log.info("calling H2 version of StoreProcedure postKeyDateAdjustmentUpsert")
  }

  override fun preKeyDateAdjustmentDeletion(
    keyDateAdjustmentId: Long,
    bookingId: Long,
  ) {
    log.info("calling H2 version of StoreProcedure preKeyDateAdjustmentDeletion")
  }

  override fun audit(name: String) {
    log.info("calling H2 version of StoreProcedure audit")
  }

  override fun clearAudit() {
    log.info("calling H2 version of StoreProcedure clearAudit")
  }
}
