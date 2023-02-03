package uk.gov.justice.digital.hmpps.nomisprisonerapi.repository

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.KeyDateAdjustmentDelete
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.KeyDateAdjustmentInsert

interface StoredProcedureRepository {
  fun postKeyDateAdjustmentUpsert(
    keyDateAdjustmentId: Long,
    bookingId: Long
  )

  fun preKeyDateAdjustmentDeletion(
    keyDateAdjustmentId: Long,
    bookingId: Long
  )
}

@Repository
@Profile("oracle")
class StoredProcedureRepositoryOracle(
  private val keyDateAdjustmentInsertProcedure: KeyDateAdjustmentInsert,
  private val keyDateAdjustmentDeleteProcedure: KeyDateAdjustmentDelete,
) :
  StoredProcedureRepository {

  override fun postKeyDateAdjustmentUpsert(
    keyDateAdjustmentId: Long,
    bookingId: Long
  ) {
    val params = MapSqlParameterSource()
      .addValue("p_offbook_id", bookingId)
      .addValue("p_key_date_id", keyDateAdjustmentId)
    keyDateAdjustmentInsertProcedure.execute(params)
  }
  override fun preKeyDateAdjustmentDeletion(
    keyDateAdjustmentId: Long,
    bookingId: Long
  ) {
    val params = MapSqlParameterSource()
      .addValue("p_offender_book_id", bookingId)
      .addValue("p_offender_key_date_adjust_id", keyDateAdjustmentId)
    keyDateAdjustmentDeleteProcedure.execute(params)
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
    bookingId: Long
  ) {
    log.info("calling H2 version of StoreProcedure repo")
  }

  override fun preKeyDateAdjustmentDeletion(
    keyDateAdjustmentId: Long,
    bookingId: Long
  ) {
    log.info("calling H2 version of StoreProcedure repo")
  }
}
