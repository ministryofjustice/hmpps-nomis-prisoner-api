package uk.gov.justice.digital.hmpps.nomisprisonerapi.audit

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.sql.Connection

interface AuditConnectionHandler {
  fun applyAuditModule(conn: Connection)
}

@Component
@Profile("!oracle")
class H2AuditConnectionHandler : AuditConnectionHandler {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun applyAuditModule(conn: Connection) {
    val module = AuditContextHolder.get()
    log.info("Applying H2 AuditConnectionHandler with audit module $module")
  }
}

@Component
@Profile("oracle")
class OracleAuditConnectionHandler : AuditConnectionHandler {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun applyAuditModule(conn: Connection) {
    val module = AuditContextHolder.get()
    log.info("Applying Oracle AuditConnectionHandler with audit module $module")
    conn.prepareCall("{call OMS_OWNER.nomis_context.set_context(?, ?)}").use { stmt ->
      stmt.setString(1, "app.audit_module")
      stmt.setString(2, module)
      stmt.execute()
    }
  }
}
