package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
@DiscriminatorValue(PersonInternetAddress.TYPE)
class PersonInternetAddress : InternetAddress() {
  @JoinColumn(name = "OWNER_ID")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var person: Person? = null

  companion object {
    const val TYPE = "PER"
  }
}
