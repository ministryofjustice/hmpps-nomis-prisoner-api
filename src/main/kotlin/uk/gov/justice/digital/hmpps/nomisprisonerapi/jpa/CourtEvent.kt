package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "COURT_EVENTS")
@EntityOpen
class CourtEvent(
  @SequenceGenerator(
    name = "EVENT_ID",
    sequenceName = "EVENT_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "EVENT_ID")
  @Id
  @Column(name = "EVENT_ID")
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "CASE_ID")
  val courtCase: CourtCase?,

  var eventDate: LocalDate,

  var startTime: LocalDateTime,

  // db comments state this should be part of EVENT_SUBTYP reference domain - not correct
  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + MovementReason.MOVE_RSN + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "COURT_EVENT_TYPE", referencedColumnName = "code")),
    ],
  )
  var courtEventType: MovementReason,

  // only 50 last used in 2009
  val judgeName: String? = null,

  // optional and without referential integrity on DB but no nulls and all reference codes 100% match
  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + EventStatus.EVENT_STS + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "EVENT_STATUS", referencedColumnName = "code")),
    ],
  )
  var eventStatus: EventStatus,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  var prison: AgencyLocation,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "outcome_reason_code")
  var outcomeReasonCode: OffenceResultCode? = null,

  val commentText: String? = null,

  // 1 null in production
  @Convert(converter = YesNoConverter::class)
  val nextEventRequestFlag: Boolean? = false,

  // No 'Y' in production
  @Convert(converter = YesNoConverter::class)
  val orderRequestedFlag: Boolean? = false,

  var nextEventDate: LocalDate? = null,

  var nextEventStartTime: LocalDateTime? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + DirectionType.MOVE_DIRECT + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "DIRECTION_CODE", referencedColumnName = "code")),
    ],
  )
  val directionCode: DirectionType? = null,

  @Convert(converter = YesNoConverter::class)
  val holdFlag: Boolean? = null,

  @OneToMany(mappedBy = "id.courtEvent", cascade = [CascadeType.ALL], orphanRemoval = true)
  var courtEventCharges: MutableList<CourtEventCharge> = mutableListOf(),

  @OneToMany(mappedBy = "courtEvent", cascade = [CascadeType.ALL], orphanRemoval = true)
  val courtOrders: MutableList<CourtOrder> = mutableListOf(),

  /* COLUMNS NOT MAPPED
    END_TIME - not used
    EVENT_OUTCOME - not used
    RESULT_CODE - not used
    OUTCOME_DATE - not used
    OFFENDER_PROCEEDING_ID - not used
   */

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
    other as CourtEvent
    return id == other.id
  }

  fun isLatestAppearance(): Boolean {
    return courtCase?.let {
      this == courtCase!!.courtEvents.sortedBy { event ->
        LocalDateTime.of(
          event.eventDate,
          event.startTime.toLocalTime(),
        )
      }.last()
    } ?: false
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
