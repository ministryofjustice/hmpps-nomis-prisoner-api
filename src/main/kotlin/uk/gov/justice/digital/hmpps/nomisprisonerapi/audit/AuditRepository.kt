package uk.gov.justice.digital.hmpps.nomisprisonerapi.audit

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
class AuditRepository(@Autowired private val dataSource: DataSource, @Autowired private val handler: AuditConnectionHandler) {
  fun setAdditionalInfo(additionalInfo: String) {
    handler.applyAuditAdditionalInfo(DataSourceUtils.getConnection(dataSource), additionalInfo)
  }
  fun clearAdditionalInfo() {
    handler.applyAuditAdditionalInfo(DataSourceUtils.getConnection(dataSource))
  }

  fun <T> withAdditionalInfo(additionalInfo: String, block: () -> T): T {
    setAdditionalInfo(additionalInfo)
    return block().also { clearAdditionalInfo() }
  }
}
