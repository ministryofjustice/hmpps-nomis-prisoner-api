package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
@DiscriminatorValue(AgencyLocationInternetAddress.TYPE)
class AgencyLocationInternetAddress(
  @JoinColumn(name = "OWNER_CODE")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val agencyLocation: AgencyLocation,
  internetAddress: String,
  internetAddressClass: String,
) : InternetAddress(
  internetAddress = internetAddress,
  internetAddressClass = internetAddressClass,
) {
  companion object {
    const val TYPE = "AGY"
  }
}
