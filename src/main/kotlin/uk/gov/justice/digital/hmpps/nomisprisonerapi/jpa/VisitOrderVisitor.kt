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
import org.hibernate.type.YesNoConverter
import java.util.Objects

@Entity
@Table(name = "OFFENDER_VO_VISITORS")
data class VisitOrderVisitor(
  @SequenceGenerator(name = "OFFENDER_VO_VISITOR_ID", sequenceName = "OFFENDER_VO_VISITOR_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_VO_VISITOR_ID")
  @Id
  @Column(name = "OFFENDER_VO_VISITOR_ID", nullable = false)
  val id: Long = 0,

  @ManyToOne(optional = false)
  @JoinColumn(name = "OFFENDER_VISIT_ORDER_ID", nullable = false)
  val visitOrder: VisitOrder,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "PERSON_ID", nullable = false)
  val person: Person,

  @Column(name = "GROUP_LEADER_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val groupLeader: Boolean = false,
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
