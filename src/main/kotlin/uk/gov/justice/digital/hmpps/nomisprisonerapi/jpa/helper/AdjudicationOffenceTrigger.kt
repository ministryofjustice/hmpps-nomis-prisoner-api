package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper

import java.math.BigDecimal
import java.sql.Connection
import kotlin.random.Random

/**
 * Fake H2 trigger to set lids_charge_number - in real trigger this would be based on charges seq per year
 */
class AdjudicationOffenceTrigger : org.h2.api.Trigger {
  override fun fire(conn: Connection, oldRow: Array<out Any>?, newRow: Array<out Any>) {
    val incidentId = newRow[0] as BigDecimal
    val chargeSequence = newRow[1] as BigDecimal
    val statement = conn.prepareStatement("UPDATE AGENCY_INCIDENT_CHARGES SET LIDS_CHARGE_NUMBER = ? WHERE AGENCY_INCIDENT_ID = ? AND CHARGE_SEQ = ?")
    statement.setInt(1, Random.nextInt(999999))
    statement.setBigDecimal(2, incidentId)
    statement.setBigDecimal(3, chargeSequence)
    statement.executeUpdate()
  }
}
