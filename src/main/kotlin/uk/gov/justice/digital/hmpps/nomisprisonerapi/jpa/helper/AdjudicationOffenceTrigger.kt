package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper

import java.math.BigDecimal
import java.sql.Connection

/**
 * Fake H2 trigger to set OIC_CHARGE_ID - in real trigger this would be based on Adjudication number
 */
class AdjudicationOffenceTrigger : org.h2.api.Trigger {
  override fun fire(conn: Connection, oldRow: Array<out Any>?, newRow: Array<out Any>) {
    val incidentId = newRow[0] as BigDecimal
    val chargeSequence = newRow[1] as BigDecimal
    val statement = conn.prepareStatement("UPDATE AGENCY_INCIDENT_CHARGES SET OIC_CHARGE_ID = ? WHERE AGENCY_INCIDENT_ID = ? AND CHARGE_SEQ = ?")
    statement.setString(1, "$incidentId/$chargeSequence")
    statement.setBigDecimal(2, incidentId)
    statement.setBigDecimal(3, chargeSequence)
    val rows = statement.executeUpdate()
  }
}
