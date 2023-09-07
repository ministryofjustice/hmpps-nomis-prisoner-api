package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
class AdjudicationHearingNotificationId(
  @Column(name = "OIC_HEARING_ID", nullable = false)
  var oicHearingId: Long,

  @Column(name = "OIC_NOTICE_SEQ", nullable = false)
  var noticeSequence: Int,
) : Serializable

@Entity
@Table(name = "OIC_HEARING_NOTICES")
class AdjudicationHearingNotification(

  @EmbeddedId
  val id: AdjudicationHearingNotificationId,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OIC_HEARING_ID", insertable = false, updatable = false)
  val hearing: AdjudicationHearing,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "DELIVERY_STAFF_ID")
  val deliveryStaff: Staff,

  @Column(name = "DELIVERY_DATE")
  val deliveryDate: LocalDate = LocalDate.now(),

  @Column(name = "DELIVERY_TIME")
  val deliveryDateTime: LocalDateTime = LocalDateTime.now(),

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationHearingNotification
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
