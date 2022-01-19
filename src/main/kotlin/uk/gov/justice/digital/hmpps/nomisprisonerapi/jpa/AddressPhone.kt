package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
@DiscriminatorValue(AddressPhone.PHONE_TYPE)
data class AddressPhone(

  @JoinColumn(name = "OWNER_ID")
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  var address: Address
) : Phone() {

  constructor(address: Address, phoneId: Long, phoneType: String?, phoneNo: String?, extNo: String?) : this(address) {
    this.phoneId = phoneId
    this.phoneType = phoneType
    this.phoneNo = phoneNo
    this.extNo = extNo
  }

  companion object {
    const val PHONE_TYPE = "ADDR"
  }
}
