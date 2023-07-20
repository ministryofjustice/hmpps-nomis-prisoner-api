package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper

import org.h2.api.Trigger
import java.math.BigDecimal
import java.sql.Connection

/**
 * Fake H2 trigger to emulate COURSE_ACTIVITIES_T2.trg
 */
class CourseActivityTrigger : Trigger {
  override fun fire(conn: Connection, oldRow: Array<out Any>?, newRow: Array<out Any>) {
    val courseActivityId = newRow[0] as BigDecimal
    val statement = conn.prepareStatement("INSERT into COURSE_ACTIVITY_AREAS (CRS_ACTY_ID, AREA_CODE) values (?, ?)")
    statement.setBigDecimal(1, courseActivityId)
    statement.setString(2, "AREA")
    statement.executeUpdate()
  }
}
