package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
data class OffenderImprisonmentStatusId(
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "IMPRISON_STATUS_SEQ", nullable = false)
  val sequence: Long,
) : Serializable

@Entity
@Table(name = "OFFENDER_IMPRISON_STATUSES")
@EntityOpen
class OffenderImprisonmentStatus(
  @EmbeddedId
  var id: OffenderImprisonmentStatusId,

  @Column(name = "IMPRISONMENT_STATUS")
  var statusCode: String,

  // several values of statusCode are not in the ImprisonmentStatus table, so we ignore the missing reference
  @ManyToOne(fetch = LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumn(name = "IMPRISONMENT_STATUS", referencedColumnName = "IMPRISONMENT_STATUS", insertable = false, updatable = false)
  var status: ImprisonmentStatus? = null,

  @Column(name = "EFFECTIVE_DATE")
  var effectiveDate: LocalDate,

  @Column(name = "EFFECTIVE_TIME")
  var effectiveTime: LocalDateTime,

  @Column(name = "EXPIRY_DATE")
  var expiryDate: LocalDate? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  var prison: AgencyLocation,

  @Column(name = "CREATE_DATE")
  var createDate: LocalDate? = null,

  @Column(name = "COMMENT_TEXT")
  var commentText: String? = null,

  @Convert(converter = YesNoConverter::class)
  @Column(name = "LATEST_STATUS", nullable = false)
  var latestStatus: Boolean,

) : NomisAuditableEntityBasic() {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderImprisonmentStatus

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
