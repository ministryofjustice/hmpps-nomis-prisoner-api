package uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs

import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.simple.SimpleJdbcCall
import org.springframework.stereotype.Component
import java.sql.Types
import javax.sql.DataSource

@Component
class KeyDateAdjustmentInsert(dataSource: DataSource) : SimpleJdbcCall(dataSource) {
  init {
    withSchemaName("OMS_OWNER")
      .withCatalogName("TAG_SENTENCE_CALC")
      .withProcedureName("insert_adjust_days")
      .withoutProcedureColumnMetaDataAccess()
      .withNamedBinding()
      .declareParameters(
        SqlParameter("p_key_date_id", Types.NUMERIC),
        SqlParameter("p_off_book_id", Types.NUMERIC)
      )
    compile()
  }
}
