package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Embeddable
data class OffenderExternalMovementId(
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "MOVEMENT_SEQ", nullable = false)
  val sequence: Long,
) : Serializable

@EntityOpen
@Entity
@Table(name = "OFFENDER_EXTERNAL_MOVEMENTS")
class OffenderExternalMovement(
  @EmbeddedId
  val id: OffenderExternalMovementId,

  @Basic
  @Column(name = "MOVEMENT_DATE")
  val movementDate: LocalDate,

  @Column(name = "MOVEMENT_TIME")
  val movementTime: LocalDateTime,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + MovementType.MOVE_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "MOVEMENT_TYPE", referencedColumnName = "code")),
    ],
  )
  val movementType: MovementType,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + MovementReason.MOVE_RSN + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "MOVEMENT_REASON_CODE", referencedColumnName = "code")),
    ],
  )
  val movementReason: MovementReason,

  @Column(name = "DIRECTION_CODE")
  @Enumerated(EnumType.STRING)
  var movementDirection: MovementDirection,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "FROM_AGY_LOC_ID")
  val fromAgency: AgencyLocation? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "TO_AGY_LOC_ID")
  val toAgency: AgencyLocation? = null,

  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  var active: Boolean = false,

  @Column(name = "COMMENT_TEXT")
  var commentText: String? = null,

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
    other as OffenderExternalMovement
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}

enum class MovementDirection {
  IN,
  OUT,
}
