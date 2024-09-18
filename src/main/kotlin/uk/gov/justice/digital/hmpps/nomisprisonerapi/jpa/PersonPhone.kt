package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
@DiscriminatorValue(PersonPhone.PHONE_TYPE)
class PersonPhone(
  @JoinColumn(name = "OWNER_ID")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val person: Person,
  phoneType: PhoneUsage,
  phoneNo: String,
  extNo: String? = null,
) : Phone(phoneType = phoneType, phoneNo = phoneNo, extNo = extNo) {
  companion object {
    const val PHONE_TYPE = "PER"
  }
}
