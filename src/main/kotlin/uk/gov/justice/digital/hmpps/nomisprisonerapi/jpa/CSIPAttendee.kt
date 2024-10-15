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
import org.hibernate.annotations.Generated
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_CSIP_ATTENDEES")
@EntityOpen
class CSIPAttendee(
  @Id
  @Column(name = "ATTENDEE_ID")
  @SequenceGenerator(name = "ATTENDEE_ID", sequenceName = "ATTENDEE_ID", allocationSize = 1)
  @GeneratedValue(generator = "ATTENDEE_ID")
  val id: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REVIEW_ID")
  val csipReview: CSIPReview,

  @Column(name = "ATTENDEE_NAME")
  var name: String? = null,

  @Column(name = "ATTENDEE_ROLE")
  var role: String? = null,

  @Column(name = "ATTENDED")
  @Convert(converter = YesNoConverter::class)
  var attended: Boolean = false,

  @Column(name = "CONTRIBUTION")
  var contribution: String? = null,

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
    other as CSIPAttendee

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id)"
  }
}
