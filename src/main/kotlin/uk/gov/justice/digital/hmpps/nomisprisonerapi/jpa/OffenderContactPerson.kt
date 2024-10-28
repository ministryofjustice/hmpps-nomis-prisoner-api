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
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import java.time.LocalDate

@Entity
@Table(name = "OFFENDER_CONTACT_PERSONS")
data class OffenderContactPerson(
  @Id
  @SequenceGenerator(
    name = "OFFENDER_CONTACT_PERSON_ID",
    sequenceName = "OFFENDER_CONTACT_PERSON_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "OFFENDER_CONTACT_PERSON_ID")
  @Column(name = "OFFENDER_CONTACT_PERSON_ID", nullable = false)
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @ManyToOne(optional = true, fetch = LAZY)
  @JoinColumn(name = "PERSON_ID")
  val person: Person?,

  @ManyToOne(optional = true, fetch = LAZY)
  @JoinColumn(name = "CONTACT_ROOT_OFFENDER_ID")
  val rootOffender: Offender?,

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + RelationshipType.RELATIONSHIP + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "RELATIONSHIP_TYPE", referencedColumnName = "code")),
    ],
  )
  val relationshipType: RelationshipType,

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + ContactType.CONTACTS + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "CONTACT_TYPE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val contactType: ContactType,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = false,

  @Column(name = "NEXT_OF_KIN_FLAG")
  @Convert(converter = YesNoConverter::class)
  val nextOfKin: Boolean = false,

  @Column(name = "EMERGENCY_CONTACT_FLAG")
  @Convert(converter = YesNoConverter::class)
  val emergencyContact: Boolean = false,

  // handful of null values
  @Column(name = "APPROVED_VISITOR_FLAG")
  @Convert(converter = YesNoConverter::class)
  val approvedVisitor: Boolean? = false,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @Column(name = "EXPIRY_DATE")
  val expiryDate: LocalDate? = null,

  @OneToMany(mappedBy = "contactPerson", cascade = [CascadeType.ALL], fetch = LAZY)
  val restrictions: MutableList<OffenderPersonRestrict> = mutableListOf(),

  /*
  Not mapped:
  CASELOAD_TYPE - always null
  CASE_INFO_NUMBER - always null
  AWARE_OF_CHARGES_FLAG - always default of N
  CAN_BE_CONTACTED_FLAG - always default of N
   */
) : NomisAuditableEntity() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderContactPerson

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $id )"
}
