package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
@DiscriminatorValue(CorporateInternetAddress.TYPE)
class CorporateInternetAddress(
  @JoinColumn(name = "OWNER_ID")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val corporate: Corporate,
  internetAddress: String,
  internetAddressClass: String,
) : InternetAddress(
  internetAddress = internetAddress,
  internetAddressClass = internetAddressClass,
) {
  companion object {
    const val TYPE = "CORP"
    const val WEB = "WEB"
    const val EMAIL = "EMAIL"
  }
}
