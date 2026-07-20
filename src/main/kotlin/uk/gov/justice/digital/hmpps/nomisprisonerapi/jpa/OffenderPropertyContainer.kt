package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import java.time.LocalDate

@Entity
@Table(name = "OFFENDER_PPTY_CONTAINERS")
data class OffenderPropertyContainer(
  @Id
  @SequenceGenerator(name = "PROPERTY_CONTAINER_ID", sequenceName = "PROPERTY_CONTAINER_ID", allocationSize = 1)
  @GeneratedValue(generator = "PROPERTY_CONTAINER_ID")
  val propertyContainerId: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "INTERNAL_LOCATION_ID")
  var agencyInternalLocation: AgencyInternalLocation? = null,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  val agencyLocation: AgencyLocation,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  var active: Boolean = true,

  @Column(name = "SEAL_MARK")
  var sealMark: String? = null,

  /*
865081	BULK Bulk
4174	  BRA  Branston Storage
37	    DES  For Destruction
59649	  VALU Valuables
241	    CO   Confiscated
   */
  @Enumerated(EnumType.STRING)
  var containerCode: PropertyContainerCode,

  var proposedDisposalDate: LocalDate? = null,
  val expiryDate: LocalDate? = null,
) : NomisAuditableEntityBasic() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderPropertyContainer
    return propertyContainerId == other.propertyContainerId
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

enum class PropertyContainerCode { BRA, BULK, CO, DES, VALU }
