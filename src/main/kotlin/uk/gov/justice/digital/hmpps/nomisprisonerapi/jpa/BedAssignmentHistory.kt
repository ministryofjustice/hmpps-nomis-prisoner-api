package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
data class BedAssignmentHistoryId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "BED_ASSIGN_SEQ", nullable = false)
  val sequence: Int,
) : Serializable

@Entity
@Table(name = "BED_ASSIGNMENT_HISTORIES")
class BedAssignmentHistory(
  @EmbeddedId
  val id: BedAssignmentHistoryId,

  // Kept as a raw id as well as a relationship: historic rows can reference living units that no longer exist
  @Column(name = "LIVING_UNIT_ID", nullable = false)
  val livingUnitId: Long,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumn(name = "LIVING_UNIT_ID", insertable = false, updatable = false)
  val livingUnit: AgencyInternalLocation? = null,

  @Column(name = "ASSIGNMENT_DATE", nullable = false)
  val assignmentDate: LocalDate,

  @Column(name = "ASSIGNMENT_TIME", nullable = false)
  val assignmentTime: LocalDateTime,

  @Column(name = "ASSIGNMENT_REASON")
  val assignmentReasonCode: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'${HousingChangeReason.CHG_HOUS_RSN}'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "ASSIGNMENT_REASON", referencedColumnName = "code", insertable = false, updatable = false)),
    ],
  )
  val assignmentReason: HousingChangeReason? = null,

  @Column(name = "ASSIGNMENT_END_DATE")
  var assignmentEndDate: LocalDate? = null,

  @Column(name = "ASSIGNMENT_END_TIME")
  var assignmentEndTime: LocalDateTime? = null,
) : NomisAuditableEntityBasic() {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as BedAssignmentHistory
    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
