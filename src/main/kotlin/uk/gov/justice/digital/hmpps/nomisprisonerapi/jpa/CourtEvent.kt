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

  val eventDate: LocalDate,

  val startTime: LocalDateTime,

  // db comments state this should be part of EVENT_SUBTYP reference domain - not correct
  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CourtEventType.MOVE_RSN + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "COURT_EVENT_TYPE", referencedColumnName = "code")),
    ],
  )
  val courtEventType: CourtEventType,

  // only 50 last used in 2009
  val judgeName: String?,

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
  val eventStatus: EventStatus,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val prison: AgencyLocation,

  // nullable, no reference code match, look like integer codes that do not exist - db comment implies "CANC_RSN" which doesn't exist
  val outcomeReasonCode: String?,

  val commentText: String?,

  @Convert(converter = YesNoConverter::class)
  val nextEventRequestFlag: Boolean?, // 1 null in production

  @Convert(converter = YesNoConverter::class)
  val orderRequestedFlag: Boolean?, // No 'Y' in production

  val nextEventDate: LocalDate?,

  val nextEventStartTime: LocalDateTime?,

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
  val directionCode: DirectionType?,

  @Convert(converter = YesNoConverter::class)
  val holdFlag: Boolean? = false, // nulls exist

  @OneToMany(mappedBy = "id.courtEvent", cascade = [CascadeType.ALL], orphanRemoval = true)
  val courtEventCharges: MutableList<CourtEventCharge> = mutableListOf(),

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

  override fun hashCode(): Int = javaClass.hashCode()
}
