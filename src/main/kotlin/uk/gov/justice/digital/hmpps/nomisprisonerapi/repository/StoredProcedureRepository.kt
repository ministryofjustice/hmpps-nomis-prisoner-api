package uk.gov.justice.digital.hmpps.nomisprisonerapi.repository

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.DEFAULT_AUDIT_MODULE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.AuditProcedure
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.ImprisonmentStatusUpdate
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
  fun resetAudit()

  fun imprisonmentStatusUpdate(
    bookingId: Long,
    changeType: String,
  )
}

@Repository
@Profile("oracle")
class StoredProcedureRepositoryOracle(
  private val keyDateAdjustmentUpsertProcedure: KeyDateAdjustmentUpsert,
  private val keyDateAdjustmentDeleteProcedure: KeyDateAdjustmentDelete,
  private val imprisonmentStatusUpdate: ImprisonmentStatusUpdate,
  private val auditProcedure: AuditProcedure,
) : StoredProcedureRepository {

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

  override fun imprisonmentStatusUpdate(
    bookingId: Long,
    changeType: String,
  ) {
    val params = MapSqlParameterSource()
      .addValue("p_offender_book_id", bookingId)
      .addValue("p_change_type", changeType)
    imprisonmentStatusUpdate.execute(params)
  }

  override fun audit(name: String) {
    val paramMap: SqlParameterSource = MapSqlParameterSource()
      .addValue("V_NAME", "AUDIT_MODULE_NAME")
      .addValue("V_VALUE", name)

    auditProcedure.execute(paramMap)
  }

  override fun resetAudit() {
    val paramMap: SqlParameterSource = MapSqlParameterSource()
      .addValue("V_NAME", "AUDIT_MODULE_NAME")
      .addValue("V_VALUE", DEFAULT_AUDIT_MODULE)

    auditProcedure.execute(paramMap)
  }
}

@Repository
@Profile("!oracle")
class StoredProcedureRepositoryH2 : StoredProcedureRepository {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun postKeyDateAdjustmentUpsert(
    keyDateAdjustmentId: Long,
    bookingId: Long,
  ) {
    log.info("calling H2 version of StoredProcedure postKeyDateAdjustmentUpsert")
  }

  override fun preKeyDateAdjustmentDeletion(
    keyDateAdjustmentId: Long,
    bookingId: Long,
  ) {
    log.info("calling H2 version of StoredProcedure preKeyDateAdjustmentDeletion")
  }

  override fun imprisonmentStatusUpdate(
    bookingId: Long,
    changeType: String,
  ) {
    log.info("calling H2 version of StoredProcedure imprisonmentStatusUpdate with bookingId: $bookingId and change type: $changeType")
  }

  override fun audit(name: String) {
    log.info("calling H2 version of StoredProcedure audit with value $name")
  }

  override fun resetAudit() {
    log.info("calling H2 version of StoredProcedure resetAudit")
  }
}
