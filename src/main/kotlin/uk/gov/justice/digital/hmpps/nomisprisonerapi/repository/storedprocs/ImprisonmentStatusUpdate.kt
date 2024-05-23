package uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs

import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.simple.SimpleJdbcCall
import org.springframework.stereotype.Component
import java.sql.Types
import javax.sql.DataSource

@Component
class ImprisonmentStatusUpdate(dataSource: DataSource) : SimpleJdbcCall(dataSource) {
  init {
    withSchemaName("OMS_OWNER")
      .withCatalogName("TAG_IMPRISONMENT_STATUS")
      .withProcedureName("post_message")
      .withoutProcedureColumnMetaDataAccess()
      .withNamedBinding()
      .declareParameters(
        SqlParameter("p_offender_book_id", Types.NUMERIC),
        SqlParameter("p_change_type", Types.VARCHAR),
      )
    compile()
  }
}

// TODO could these be new values or are these required for reports?
enum class ImprisonmentStatusChangeType {
  UPDATE_SENTENCE,
  UPDATE_RESULT,
  UPDATE_LEGAL_STATUS,
  DELETE,
}
