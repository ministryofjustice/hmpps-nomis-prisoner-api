package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
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
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate

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

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + VisitOrderType.VISIT_ORDER_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "VISIT_ORDER_TYPE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var visitOrderType: VisitOrderType,

/* status content seems to be mainly set to scheduled and the other options don't all make sense */
  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + VisitStatus.VISIT_STATUS + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "STATUS", referencedColumnName = "code", nullable = true)),
    ],
  )
  var status: VisitStatus,

  @OneToMany(mappedBy = "visitOrder", cascade = [CascadeType.ALL], orphanRemoval = true)
  var visitors: MutableList<VisitOrderVisitor> = mutableListOf(),

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
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(name = "OUTCOME_REASON_CODE", referencedColumnName = "code", nullable = true),
      ),
    ],
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
