package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.YesNoConverter
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "CORPORATES")
class Corporate(
  @SequenceOrUseId(name = "CORPORATE_ID")
  @Id
  @Column(name = "CORPORATE_ID", nullable = false)
  var id: Long = 0,
  @Column(name = "CORPORATE_NAME", nullable = false)
  var corporateName: String,
  @JoinColumn(name = "CASELOAD_ID")
  @ManyToOne(fetch = LAZY)
  var caseload: Caseload?,
  @Column(name = "CREATED_DATE")
  val createdDate: LocalDateTime = LocalDateTime.now(),
  @Column(name = "COMMENT_TEXT")
  var commentText: String?,
  // always N with no default
  @Column(name = "SUSPENDED_FLAG")
  @Convert(converter = YesNoConverter::class)
  var suspended: Boolean = false,
  @Column(name = "FEI_NUMBER")
  var feiNumber: String?,
  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  var active: Boolean = true,
  @Column(name = "EXPIRY_DATE")
  var expiryDate: LocalDate?,
  // nearly always null
  @Column(name = "TAX_NO")
  var taxNo: String?,

  @OneToMany(mappedBy = "id.corporate", cascade = [CascadeType.ALL], fetch = LAZY, orphanRemoval = true)
  val types: MutableList<CorporateType> = mutableListOf(),

  @OneToMany(mappedBy = "corporate", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${CorporateAddress.ADDR_TYPE}'")
  val addresses: MutableList<CorporateAddress> = mutableListOf(),

  @OneToMany(mappedBy = "corporate", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${CorporatePhone.PHONE_TYPE}'")
  val phones: MutableList<CorporatePhone> = mutableListOf(),

  @OneToMany(mappedBy = "corporate", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${CorporateInternetAddress.TYPE}'")
  val internetAddresses: MutableList<CorporateInternetAddress> = mutableListOf(),

  /*
  Not mapped:
  CONTACT_PERSON_NAME - always null
  UPDATED_DATE - always null
  USER_ID - always null
  START_DATE - always null
  ACCOUNT_TERM_CODE - always null
  SHIPPING_TERM_CODE - always null
  MINIMUM_PURCHASE_AMOUNT  - always null
  MAXIMUM_PURCHASE_AMOUNT  - always null
  MEMO_TEXT - always null
  SUSPENDED_DATE - always null
   */
) : NomisAuditableEntityWithStaff() {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Corporate

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
