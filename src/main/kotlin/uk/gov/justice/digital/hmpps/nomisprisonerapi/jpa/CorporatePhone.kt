package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
@DiscriminatorValue(CorporatePhone.PHONE_TYPE)
class CorporatePhone(
  @JoinColumn(name = "OWNER_ID")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val corporate: Corporate,
  phoneType: PhoneUsage,
  phoneNo: String,
  extNo: String? = null,
) : Phone(phoneType = phoneType, phoneNo = phoneNo, extNo = extNo) {
  companion object {
    const val PHONE_TYPE = "CORP"
  }
}
