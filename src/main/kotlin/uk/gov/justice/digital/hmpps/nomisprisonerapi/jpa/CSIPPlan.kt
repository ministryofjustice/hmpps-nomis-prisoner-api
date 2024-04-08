package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_CSIP_PLANS")
@EntityOpen
data class CSIPPlan(
  @Id
  @Column(name = "PLAN_ID")
  @SequenceGenerator(name = "PLAN_ID", sequenceName = "PLAN_ID", allocationSize = 1)
  @GeneratedValue(generator = "PLAN_ID")
  val id: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CSIP_ID")
  val csipReport: CSIPReport,

  @Column(name = "IDENTIFIED_NEED", nullable = false)
  val identifiedNeed: String,

  @Column(name = "BY_WHOM", nullable = false)
  val referredBy: String,

  @Column(name = "CREATE_DATE", nullable = false)
  val createDate: LocalDate = LocalDate.now(),

  @Column(name = "TARGET_DATE", nullable = false)
  val targetDate: LocalDate = LocalDate.now(),

  @Column(name = "CLOSED_DATE")
  val closedDate: LocalDate = LocalDate.now(),

  @Column(name = "INTERVENTION", nullable = false)
  val intervention: String,

  @Column(name = "PROGRESSION")
  val progression: String?,

) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CSIPPlan

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id)"
  }
}
