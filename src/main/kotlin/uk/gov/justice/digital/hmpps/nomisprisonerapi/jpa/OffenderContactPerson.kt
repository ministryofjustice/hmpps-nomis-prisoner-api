package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction.IGNORE
import org.hibernate.type.YesNoConverter

@Entity
@Table(name = "OFFENDER_CONTACT_PERSONS")
data class OffenderContactPerson(
  @Id
  @SequenceGenerator(
    name = "OFFENDER_CONTACT_PERSON_ID",
    sequenceName = "OFFENDER_CONTACT_PERSON_ID",
    allocationSize = 1
  )
  @GeneratedValue(generator = "OFFENDER_CONTACT_PERSON_ID")
  @Column(name = "OFFENDER_CONTACT_PERSON_ID", nullable = false)
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "PERSON_ID")
  val person: Person,

  @ManyToOne
  @NotFound(action = IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + RelationshipType.RELATIONSHIP + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "RELATIONSHIP_TYPE", referencedColumnName = "code"))
    ]
  )
  val relationshipType: RelationshipType,

  @ManyToOne
  @NotFound(action = IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + ContactType.CONTACTS + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "CONTACT_TYPE",
          referencedColumnName = "code",
          nullable = true
        )
      )
    ]
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

  @Column(name = "APPROVED_VISITOR_FLAG")
  @Convert(converter = YesNoConverter::class)
  val approvedVisitor: Boolean = false,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderContactPerson

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id )"
  }
}
