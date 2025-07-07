package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Basic
import jakarta.persistence.CascadeType
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
import jakarta.persistence.OneToOne
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
  val movementType: MovementType? = null,

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

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + ArrestAgency.ARREST_AGY + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "ARREST_AGENCY_LOC_ID", referencedColumnName = "code")),
    ],
  )
  val arrestAgency: ArrestAgency? = null,

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${Escort.ESCORT}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "ESCORT_CODE", referencedColumnName = "code")),
    ],
  )
  val escort: Escort? = null,

  @Column(name = "ESCORT_TEXT")
  val escortText: String? = null,

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

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + City.CITY + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "FROM_CITY", referencedColumnName = "code")),
    ],
  )
  var fromCity: City? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + City.CITY + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "TO_CITY", referencedColumnName = "code")),
    ],
  )
  var toCity: City? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "FROM_ADDRESS_ID")
  val fromAddress: Address? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "TO_ADDRESS_ID")
  val toAddress: Address? = null,

  @OneToOne(cascade = [CascadeType.ALL])
  @JoinColumn(name = "EVENT_ID")
  var scheduledMovement: OffenderScheduledExternalMovement? = null,
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
