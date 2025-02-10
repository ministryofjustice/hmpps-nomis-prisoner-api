package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime

enum class EventClass {
  EXT_MOV,
  INT_MOV,
  COMM,
}

@Entity
@Table(name = "OFFENDER_IND_SCHEDULES")
class OffenderIndividualSchedule(

  @Id
  @SequenceGenerator(name = "EVENT_ID", sequenceName = "EVENT_ID", allocationSize = 1)
  @GeneratedValue(generator = "EVENT_ID")
  @Column(name = "EVENT_ID", nullable = false)
  val eventId: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column
  var eventDate: LocalDate? = null,

  @Column
  var startTime: LocalDateTime? = null,

  @Column
  var endTime: LocalDateTime? = null,

  // INT_MOV only for now
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val eventClass: EventClass = EventClass.INT_MOV,

  // APP for appointment
  @Column(nullable = false)
  val eventType: String = "APP",

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${EventSubType.INT_SCH_RSN}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_SUB_TYPE", referencedColumnName = "code")),
    ],
  )
  var eventSubType: EventSubType,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${EventStatus.EVENT_STS}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_STATUS", referencedColumnName = "code")),
    ],
  )
  var eventStatus: EventStatus,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  val prison: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_AGY_LOC_ID")
  val toPrison: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_INTERNAL_LOCATION_ID")
  var internalLocation: AgencyInternalLocation? = null,

  @Column(name = "COMMENT_TEXT")
  var comment: String? = null,

  @Column(name = "CREATE_DATETIME", nullable = false, updatable = false, insertable = false)
  @CreatedDate
  val createdDate: LocalDateTime? = null,

  @Column(name = "CREATE_USER_ID", nullable = false, updatable = false, insertable = false)
  @CreatedBy
  val createdBy: String? = null,

  @Column(name = "MODIFY_DATETIME", updatable = false, insertable = false)
  @LastModifiedDate
  val modifiedDate: LocalDateTime? = null,

  @Column(name = "MODIFY_USER_ID", updatable = false, insertable = false)
  @LastModifiedBy
  val modifiedBy: String? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderIndividualSchedule

    return eventId == other.eventId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $eventId, booking = ${offenderBooking.bookingId}, eventDate = $eventDate, startTime = $startTime)"
}
