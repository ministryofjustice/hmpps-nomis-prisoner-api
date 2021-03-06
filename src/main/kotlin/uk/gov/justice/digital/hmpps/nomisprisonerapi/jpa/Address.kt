package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.Where
import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.DiscriminatorColumn
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "ADDRESSES")
@DiscriminatorColumn(name = "OWNER_CLASS")
@Inheritance
abstract class Address(
  val premise: String? = null,
  val street: String? = null,
  val locality: String? = null,
  @Column(name = "START_DATE")
  val startDate: LocalDate = LocalDate.now(),
  @Column(name = "NO_FIXED_ADDRESS_FLAG")
  val noFixedAddressFlag: String = "N",
  @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  @Where(clause = "OWNER_CLASS = '${AddressPhone.PHONE_TYPE}'")
  val phones: MutableList<AddressPhone> = ArrayList()
) {
  @Id
  @SequenceGenerator(name = "ADDRESS_ID", sequenceName = "ADDRESS_ID", allocationSize = 1)
  @GeneratedValue(generator = "ADDRESS_ID")
  @Column(name = "ADDRESS_ID", nullable = false)
  open val addressId: Long = 0

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AddressType.ADDR_TYPE + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "ADDRESS_TYPE", referencedColumnName = "code"))
    ]
  )
  open val addressType: AddressType? = null
  open val flat: String? = null

  @Column(name = "POSTAL_CODE")
  open val postalCode: String? = null

  @Column(name = "PRIMARY_FLAG", nullable = false)
  open val primaryFlag = "N"

  @Column(name = "MAIL_FLAG", nullable = false)
  open val mailFlag = "N"

  @Column(name = "COMMENT_TEXT")
  open val commentText: String? = null

  @Column(name = "END_DATE")
  open val endDate: LocalDate? = null

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + County.COUNTY + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "COUNTY_CODE", referencedColumnName = "code"))
    ]
  )
  open val county: County? = null

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + City.CITY + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "CITY_CODE", referencedColumnName = "code"))
    ]
  )
  open val city: City? = null

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + Country.COUNTRY + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "COUNTRY_CODE", referencedColumnName = "code"))
    ]
  )
  open val country: Country? = null

  @OneToMany
  @JoinColumn(name = "ADDRESS_ID")
  open val addressUsages: List<AddressUsage> = ArrayList()

  fun removePhone(phone: AddressPhone) {
    phones.remove(phone)
  }

  fun addPhone(phone: AddressPhone): AddressPhone {
    phone.address = this
    phones.add(phone)
    return phone
  }

  override fun equals(other: Any?): Boolean {

    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Address
    return addressId == other.addressId
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
