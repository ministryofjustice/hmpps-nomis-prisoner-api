package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
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
import org.hibernate.annotations.NotFoundAction
import org.hibernate.type.YesNoConverter

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
  @Convert(converter = YesNoConverter::class)
  val groupLeader: Boolean = false,

  @Column(name = "ASSISTED_VISIT_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val assistedVisit: Boolean = false,

  @Column
  val commentText: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + EventStatus.EVENT_STS + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_STATUS", referencedColumnName = "code")),
    ],
  )
  var eventStatus: EventStatus? = null,

  @Column
  val eventId: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + VisitOutcomeReason.VISIT_OUTCOME_REASON + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "OUTCOME_REASON_CODE",
          referencedColumnName = "code",
          nullable = true,
          updatable = false,
          insertable = false,
        ),
      ),
    ],
  )
  var outcomeReason: VisitOutcomeReason? = null,

  @Column(name = "OUTCOME_REASON_CODE")
  var outcomeReasonCode: String? = null,

  /* DB constraint exists: EVENT_OUTCOME IN ('ATT', 'ABS', 'CANC') */
  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + EventOutcome.EVENT_OUTCOME + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "EVENT_OUTCOME",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
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
