package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.OneToOne
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen

@Entity
@EntityOpen
@DiscriminatorValue(AlertWorkFlow.WORKFLOW_TYPE)
class AlertWorkFlow(
  @OneToOne
  @JoinColumns(
    value = [
      JoinColumn(
        referencedColumnName = "OFFENDER_BOOK_ID",
        name = "OBJECT_ID",
      ),
      JoinColumn(
        referencedColumnName = "ALERT_SEQ",
        name = "OBJECT_SEQ",
      ),
    ],
  )
  val alert: OffenderAlert,
) : WorkFlow() {
  companion object {
    const val WORKFLOW_TYPE = "ALERT"
  }
}
