package uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.storedprocs

import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.simple.SimpleJdbcCall
import org.springframework.stereotype.Component
import java.sql.Types
import javax.sql.DataSource

@Component
class AuditProcedure(dataSource: DataSource) : SimpleJdbcCall(dataSource) {
  init {
    withSchemaName("OMS_OWNER")
      .withCatalogName("nomis_context")
      .withProcedureName("set_context")
      .withoutProcedureColumnMetaDataAccess()
      .withNamedBinding()
      .declareParameters(
        SqlParameter("V_NAME", Types.VARCHAR),
        SqlParameter("V_VALUE", Types.VARCHAR),
      )
      .compile()
  }
}

@Component
class ClearAuditProcedure(dataSource: DataSource) : SimpleJdbcCall(dataSource) {
  init {
    withSchemaName("OMS_OWNER")
      .withCatalogName("nomis_context")
      .withProcedureName("set_nomis_context")
      .withoutProcedureColumnMetaDataAccess()
      .compile()
  }
}
