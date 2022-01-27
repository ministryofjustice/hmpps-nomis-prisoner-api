package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.Type
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "OFFENDER_VISIT_VISITORS")
data class VisitVisitor(
  @SequenceGenerator(name = "OFFENDER_VISIT_VISITOR_ID", sequenceName = "OFFENDER_VISIT_VISITOR_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_VISIT_VISITOR_ID")
  @Id
  @Column(name = "OFFENDER_VISIT_VISITOR_ID", nullable = false)
  val id: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_VISIT_ID", nullable = false)
  val visit: Visit,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "PERSON_ID")
  val person: Person? = null,

  @Column(name = "GROUP_LEADER_FLAG", nullable = false)
  @Type(type = "yes_no")
  val groupLeader: Boolean = false,

  @Column(name = "ASSISTED_VISIT_FLAG", nullable = false)
  @Type(type = "yes_no")
  val assistedVisit: Boolean = false,

  @Column
  val commentText: String? = null,

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + EventStatus.EVENT_STS + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_STATUS", referencedColumnName = "code"))
    ]
  )
  var eventStatus: EventStatus? = null,

  @Column
  val eventId: Long? = null,

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + VisitOutcomeReason.VISIT_OUTCOME_REASON + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "OUTCOME_REASON_CODE",
          referencedColumnName = "code",
          nullable = false
        )
      )
    ]
  )
  var outcomeReason: VisitOutcomeReason? = null,

  /* DB constraint exists: EVENT_OUTCOME IN ('ATT', 'ABS', 'CANC') */
  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + EventOutcome.EVENT_OUTCOME + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "EVENT_OUTCOME",
          referencedColumnName = "code",
          nullable = false
        )
      )
    ]
  )
  var eventOutcome: EventOutcome? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as VisitVisitor
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
