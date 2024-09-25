package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import java.io.Serializable
import java.time.LocalDateTime

@Embeddable
data class OffenderProfileDetailId(
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "PROFILE_SEQ", nullable = false)
  val sequence: Long,

  @ManyToOne
  @JoinColumn(name = "PROFILE_TYPE", nullable = false)
  val profileType: ProfileType,
) : Serializable

@Entity
@Table(name = "OFFENDER_PROFILE_DETAILS")
data class OffenderProfileDetail(
  @EmbeddedId
  val id: OffenderProfileDetailId,

  @Column(name = "LIST_SEQ")
  val listSequence: Long,

  @ManyToOne
  @JoinColumns(
    value = [
      JoinColumn(name = "OFFENDER_BOOK_ID", referencedColumnName = "OFFENDER_BOOK_ID", nullable = false, insertable = false, updatable = false),
      JoinColumn(name = "PROFILE_SEQ", referencedColumnName = "PROFILE_SEQ", nullable = false, insertable = false, updatable = false),
    ],
  )
  val offenderProfile: OffenderProfile,

  @Column(name = "PROFILE_CODE")
  var profileCodeId: String?,
) {

  @ManyToOne
  @JoinColumns(
    value = [
      JoinColumn(name = "PROFILE_TYPE", referencedColumnName = "PROFILE_TYPE", nullable = true, insertable = false, updatable = false),
      JoinColumn(name = "PROFILE_CODE", referencedColumnName = "PROFILE_CODE", nullable = true, insertable = false, updatable = false),
    ],
  )
  val profileCode: ProfileCode? = null

  @Column(name = "CREATE_DATETIME")
  @Generated
  lateinit var createDatetime: LocalDateTime

  @Column(name = "CREATE_USER_ID")
  @Generated
  lateinit var createUserId: String

  @Column(name = "MODIFY_DATETIME")
  @Generated
  var modifyDatetime: LocalDateTime? = null

  @Column(name = "MODIFY_USER_ID")
  @Generated
  var modifyUserId: String? = null

  @Column(name = "AUDIT_MODULE_NAME")
  @Generated
  var auditModuleName: String? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderProfileDetail
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(offenderBookingId = ${id.offenderBooking.bookingId}, sequence = $id.sequence, profileType = $id.profileType)"
}
