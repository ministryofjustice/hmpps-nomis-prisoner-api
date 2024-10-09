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
class CSIPPlan(
  @Id
  @Column(name = "PLAN_ID")
  @SequenceGenerator(name = "PLAN_ID", sequenceName = "PLAN_ID", allocationSize = 1)
  @GeneratedValue(generator = "PLAN_ID")
  val id: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CSIP_ID")
  val csipReport: CSIPReport,

  @Column(name = "IDENTIFIED_NEED", nullable = false)
  var identifiedNeed: String,

  @Column(name = "BY_WHOM", nullable = false)
  var referredBy: String,

  @Column(name = "CREATE_DATE", nullable = false)
  val createDate: LocalDate = LocalDate.now(),

  @Column(name = "TARGET_DATE", nullable = false)
  var targetDate: LocalDate,

  @Column(name = "CLOSED_DATE")
  var closedDate: LocalDate? = null,

  @Column(name = "INTERVENTION", nullable = false)
  var intervention: String,

  @Column(name = "PROGRESSION")
  var progression: String?,

  @Column
  var auditModuleName: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CREATE_USER_ID", insertable = false, updatable = false)
  var createdByStaffUserAccount: StaffUserAccount? = null,

  @Column(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  @Generated
  var lastModifiedUsername: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  var lastModifiedByStaffUserAccount: StaffUserAccount? = null,

  @Column(name = "MODIFY_DATETIME", insertable = false, updatable = false)
  @Generated
  var lastModifiedDateTime: LocalDateTime? = null,

  // ---- NOT MAPPED columns ---- //
  // All AUDIT data except auditModuleName
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
