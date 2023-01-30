package uk.gov.justice.digital.hmpps.nomisprisonerapi.repository

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs.KeyDateAdjustmentInsert

interface StoredProcedureRepository {
  fun postKeyDateAdjustmentCreation(
    keyDateAdjustmentId: Long,
    offBookId: Long
  )
}

@Repository
@Profile("oracle")
class StoredProcedureRepositoryOracle(private val keyDateAdjustmentProcedure: KeyDateAdjustmentInsert) :
  StoredProcedureRepository {

  override fun postKeyDateAdjustmentCreation(
    keyDateAdjustmentId: Long,
    offBookId: Long
  ) {
    val params = MapSqlParameterSource()
      .addValue("p_key_date_id", keyDateAdjustmentId)
      .addValue("p_off_book_id", offBookId)
    keyDateAdjustmentProcedure.execute(params)
  }
}

@Repository
@Profile("!oracle")
class StoredProcedureRepositoryH2() : StoredProcedureRepository {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun postKeyDateAdjustmentCreation(
    keyDateAdjustmentId: Long,
    offBookId: Long
  ) {
    log.info("calling H2 version of StoreProcedure repo")
  }
}
