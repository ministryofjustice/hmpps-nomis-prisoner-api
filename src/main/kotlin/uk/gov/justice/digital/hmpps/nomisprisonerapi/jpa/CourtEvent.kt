package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
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
  @Id
  @Column(name = "EVENT_ID")
  @SequenceOrUseId(name = "EVENT_ID")
  val id: Long = 0,

  @Column(name = "PARENT_EVENT_ID")
  var parentEventId: Long? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "CASE_ID")
  val courtCase: CourtCase? = null,

  var eventDate: LocalDate,

  private var startTime: LocalDateTime,

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
  var court: AgencyLocation,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "outcome_reason_code")
  var outcomeReasonCode: OffenceResultCode? = null,

  var commentText: String? = null,

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
  val holdFlag: Boolean? = false,

  @OneToMany(mappedBy = "id.courtEvent", cascade = [CascadeType.ALL], orphanRemoval = true)
  var courtEventCharges: MutableList<CourtEventCharge> = mutableListOf(),

  // Only ever 1 order of type "AUTO" for an event
  @OneToMany(mappedBy = "courtEvent", cascade = [CascadeType.ALL], orphanRemoval = true)
  val courtOrders: MutableList<CourtOrder> = mutableListOf(),

  /* COLUMNS NOT MAPPED
    END_TIME - not used
    EVENT_OUTCOME - not used
    RESULT_CODE - not used
    OUTCOME_DATE - not used
    OFFENDER_PROCEEDING_ID - not used
   */
) : NomisAuditableEntityWithStaff() {

  /**
   * Return the event date with the time portion set to the start time. Under some circumstances (and until
   * corrected by TAG_DATETIME_CORRECTIONS) the date portion of the start time may be different, so need to combine
   * the two to ensure we get the correct date and time.
   *
   * @return The combined LocalDateTime representing the event date and start time.
   */
  fun getEventDateAndTime(): LocalDateTime = eventDate.atTime(startTime.toLocalTime())

  fun setEventDateAndTime(eventDateTime: LocalDateTime) {
    eventDate = eventDateTime.toLocalDate()
    startTime = eventDateTime
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CourtEvent
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
