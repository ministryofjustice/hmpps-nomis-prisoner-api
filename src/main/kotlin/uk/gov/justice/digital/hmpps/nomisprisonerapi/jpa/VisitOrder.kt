package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "OFFENDER_VISIT_ORDERS")
data class VisitOrder(
  @SequenceGenerator(name = "OFFENDER_VISIT_ORDER_ID", sequenceName = "OFFENDER_VISIT_ORDER_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_VISIT_ORDER_ID")
  @Id
  @Column(name = "OFFENDER_VISIT_ORDER_ID", nullable = false)
  val id: Long = 0,

  @Column(name = "VISIT_ORDER_NUMBER", nullable = false)
  val visitOrderNumber: Long,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(nullable = false)
  val issueDate: LocalDate,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + VisitOrderType.VISIT_ORDER_TYPE + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "VISIT_ORDER_TYPE",
          referencedColumnName = "code",
          nullable = false
        )
      )
    ]
  )
  var visitOrderType: VisitOrderType,

/* status content seems to be mainly set to scheduled and the other options don't all make sense */
  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + VisitStatus.VISIT_STATUS + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "STATUS", referencedColumnName = "code", nullable = false))
    ]
  )
  var status: VisitStatus,

  @OneToMany(mappedBy = "visitOrder", cascade = [CascadeType.ALL])
  var visitors: List<VisitOrderVisitor> = ArrayList(),

  @Column
  val commentText: String? = null,

  @Column
  var expiryDate: LocalDate? = null,

  //    @ManyToOne(fetch = FetchType.LAZY)
  //    @NotFound(action = IGNORE)
  //    @JoinColumn(name = "AUTHORISED_STAFF_ID")
  //    private Staff authorisedStaffId;

  @Column
  val mailedDate: LocalDate? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${VisitOutcomeReason.VISIT_OUTCOME_REASON}'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(
        column = JoinColumn(name = "OUTCOME_REASON_CODE", referencedColumnName = "code", nullable = true)
      )
    ]
  )
  var outcomeReason: VisitOutcomeReason? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as VisitOrder
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
