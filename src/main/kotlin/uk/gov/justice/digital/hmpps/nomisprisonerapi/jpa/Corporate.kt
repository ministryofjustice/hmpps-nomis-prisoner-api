package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "CORPORATES")
class Corporate(
  @SequenceGenerator(name = "CORPORATE_ID", sequenceName = "CORPORATE_ID", allocationSize = 1)
  @GeneratedValue(generator = "CORPORATE_ID")
  @Id
  @Column(name = "CORPORATE_ID", nullable = false)
  var id: Long = 0,
  @Column(name = "CORPORATE_NAME", nullable = false)
  val corporateName: String,
  @JoinColumn(name = "CASELOAD_ID")
  @ManyToOne(fetch = LAZY)
  val caseload: Caseload?,
  @Column(name = "CREATED_DATE")
  val createdDate: LocalDateTime = LocalDateTime.now(),
  @Column(name = "COMMENT_TEXT")
  val commentText: String?,
  // always N with no default
  @Column(name = "SUSPENDED_FLAG")
  @Convert(converter = YesNoConverter::class)
  val suspended: Boolean = false,
  @Column(name = "FEI_NUMBER")
  val feiNumber: String?,
  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = true,
  @Column(name = "EXPIRY_DATE")
  val expiryDate: LocalDate?,
  // nearly always null
  @Column(name = "TAX_NO")
  val taxNo: String?,

  @OneToMany(mappedBy = "id.corporate", cascade = [CascadeType.ALL], fetch = LAZY, orphanRemoval = true)
  val types: MutableList<CorporateType> = mutableListOf(),

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
) : NomisAuditableEntity() {

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
