package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
@DiscriminatorValue(OffenderInternetAddress.TYPE)
class OffenderInternetAddress(
  @JoinColumn(name = "OWNER_ID")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val offender: Offender,
  emailAddress: String,
) : InternetAddress(
  internetAddress = emailAddress,
  internetAddressClass = EMAIL_INTERNET_ADDRESS_CLASS,
) {
  companion object {
    const val TYPE = "OFF"
  }
}
