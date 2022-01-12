package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
class PersonPhone : Phone() {
  @JoinColumn(name = "OWNER_ID")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var person: Person? = null

  companion object {
    const val PHONE_TYPE = "PER"
  }
}
