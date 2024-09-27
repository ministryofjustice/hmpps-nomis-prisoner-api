package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
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
import java.time.LocalDate

@Entity
@Table(name = "OFFENDER_PERSON_RESTRICTS")
data class OffenderPersonRestricts(
  @Id
  @SequenceGenerator(
    name = "OFFENDER_PERSON_RESTRICT_ID",
    sequenceName = "OFFENDER_PERSON_RESTRICT_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "OFFENDER_PERSON_RESTRICT_ID")
  @Column(name = "OFFENDER_PERSON_RESTRICT_ID", nullable = false)
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_CONTACT_PERSON_ID", nullable = false)
  val contactPerson: OffenderContactPerson,

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "ENTERED_STAFF_ID", nullable = true)
  val enteredStaff: Staff,

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + RestrictionType.VST_RST_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "RESTRICTION_TYPE", referencedColumnName = "code")),
    ],
  )
  val restrictionType: RestrictionType,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @Column(name = "RESTRICTION_EFFECTIVE_DATE", nullable = false)
  val effectiveDate: LocalDate,

  @Column(name = "RESTRICTION_EXPIRY_DATE", nullable = true)
  val expiryDate: LocalDate?,

  /*
  Not mapped:
  AUTHORIZED_STAFF_ID - always null
   */
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderPersonRestricts

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $id )"
}
