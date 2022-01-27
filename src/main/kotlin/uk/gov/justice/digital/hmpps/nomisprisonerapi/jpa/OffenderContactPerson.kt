package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction.IGNORE
import org.hibernate.annotations.Type
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType.LAZY
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

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
          nullable = false
        )
      )
    ]
  )
  val contactType: ContactType,

  @Column(name = "ACTIVE_FLAG")
  @Type(type = "yes_no")
  val active: Boolean = false,

  @Column(name = "NEXT_OF_KIN_FLAG")
  @Type(type = "yes_no")
  val nextOfKin: Boolean = false,

  @Column(name = "EMERGENCY_CONTACT_FLAG")
  @Type(type = "yes_no")
  val emergencyContact: Boolean = false,

  @Column(name = "APPROVED_VISITOR_FLAG")
  @Type(type = "yes_no")
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
