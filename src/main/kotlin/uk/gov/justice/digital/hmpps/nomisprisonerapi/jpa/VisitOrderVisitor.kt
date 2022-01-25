package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import org.hibernate.annotations.Type
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "OFFENDER_VO_VISITORS")
data class VisitOrderVisitor(
  @Id
  @Column(name = "OFFENDER_VO_VISITOR_ID", nullable = false)
  val id: Long,

  @ManyToOne(optional = false)
  @JoinColumn(name = "OFFENDER_VISIT_ORDER_ID", nullable = false)
  val visitOrder: VisitOrder,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "PERSON_ID", nullable = false)
  val person: Person,

  @Column(name = "GROUP_LEADER_FLAG", nullable = false)
  @Type(type = "yes_no")
  val groupLeader: Boolean = false
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as VisitOrderVisitor
    return id == other.id
  }

  override fun hashCode(): Int {
    return Objects.hashCode(id)
  }
}
