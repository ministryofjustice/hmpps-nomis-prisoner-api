package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.YesNoConverter
import java.time.LocalDate

@Entity
@Table(name = "ADDRESSES")
@DiscriminatorColumn(name = "OWNER_CLASS")
@Inheritance
abstract class Address(
  @Column(name = "PREMISE")
  open var premise: String? = null,

  @Column(name = "STREET")
  open var street: String? = null,

  @Column(name = "LOCALITY")
  open var locality: String? = null,

  @Column(name = "START_DATE")
  open var startDate: LocalDate? = null,

  @Column(name = "END_DATE")
  open var endDate: LocalDate? = null,

  @Column(name = "NO_FIXED_ADDRESS_FLAG")
  @Convert(converter = YesNoConverter::class)
  // null for a handful of addresses
  open var noFixedAddress: Boolean? = false,

  @Column(name = "PRIMARY_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  open var primaryAddress: Boolean = false,

  @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  @SQLRestriction("OWNER_CLASS = '${AddressPhone.PHONE_TYPE}'")
  open val phones: MutableList<AddressPhone> = ArrayList(),

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AddressType.ADDR_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "ADDRESS_TYPE", referencedColumnName = "code")),
    ],
  )
  open var addressType: AddressType? = null,

  @Column(name = "FLAT")
  open var flat: String? = null,

  @Column(name = "POSTAL_CODE")
  open var postalCode: String? = null,

  @ManyToOne
  // COUNTY_CODE not always found for records created 2006-01-13 08:58:25.
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + County.COUNTY + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "COUNTY_CODE", referencedColumnName = "code")),
    ],
  )
  open var county: County? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + City.CITY + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "CITY_CODE", referencedColumnName = "code")),
    ],
  )
  open var city: City? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + Country.COUNTRY + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "COUNTRY_CODE", referencedColumnName = "code")),
    ],
  )
  open var country: Country? = null,

  @Column(name = "VALIDATED_PAF_FLAG")
  @Convert(converter = YesNoConverter::class)
  open var validatedPAF: Boolean = false,

  @Column(name = "MAIL_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  open var mailAddress: Boolean = false,

  @Column(name = "COMMENT_TEXT")
  open var comment: String? = null,

  /* Not mapped
  CAPACITY - always null
  SERVICES_FLAG - always for N for personal addresses - may need mapping for corporate address
  SPECIAL_NEEDS_CODE - always null
  BUSINESS_HOUR - always null for personal addresses - may need mapping for corporate address
  CITY_NAME - always null
  ADDRESS_USAGE - not used for personal addresses
   */
) : NomisAuditableEntity() {
  @Id
  @SequenceGenerator(name = "ADDRESS_ID", sequenceName = "ADDRESS_ID", allocationSize = 1)
  @GeneratedValue(generator = "ADDRESS_ID")
  @Column(name = "ADDRESS_ID", nullable = false)
  val addressId: Long = 0

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Address
    return addressId == other.addressId
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
