package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper

import org.h2.tools.TriggerAdapter
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp

class ModifyTrigger : TriggerAdapter() {

  override fun fire(conn: Connection, oldRow: ResultSet, newRow: ResultSet) {
    newRow.updateTimestamp("MODIFY_DATETIME", Timestamp(System.currentTimeMillis()))
    newRow.updateString("MODIFY_USER_ID", "SA")
  }
}
