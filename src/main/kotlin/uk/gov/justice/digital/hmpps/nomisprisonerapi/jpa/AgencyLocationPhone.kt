package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
@DiscriminatorValue(AgencyLocationPhone.PHONE_TYPE)
class AgencyLocationPhone(
  @JoinColumn(name = "OWNER_CODE")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val agencyLocation: AgencyLocation,
  phoneType: PhoneUsage,
  phoneNo: String,
  extNo: String? = null,
) : Phone(phoneType = phoneType, phoneNo = phoneNo, extNo = extNo) {
  companion object {
    const val PHONE_TYPE = "AGY"
  }
}
