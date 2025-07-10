package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.DiscriminatorFormula
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

enum class EventClass {
  EXT_MOV,
  INT_MOV,
  COMM,
}

enum class EventType {
  APP,
  TAP,
}

@EntityOpen
@Entity
@Table(name = "OFFENDER_IND_SCHEDULES")
@DiscriminatorFormula(
  """
    case
        when EVENT_CLASS = 'INT_MOV' then 'OffenderAppointment'
        when EVENT_CLASS = 'EXT_MOV' and EVENT_TYPE = 'TAP' and DIRECTION_CODE = 'OUT' then 'OffenderScheduledTemporaryAbsence'
        when EVENT_CLASS = 'EXT_MOV' and EVENT_TYPE = 'TAP' and DIRECTION_CODE = 'IN' then 'OffenderScheduledTemporaryAbsenceReturn'
        else 'Unknown'
    end
""",
)
@Inheritance
abstract class OffenderIndividualSchedule(

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

  @Enumerated(EnumType.STRING)
  @Column(name = "EVENT_CLASS")
  val eventClass: EventClass,

  @Enumerated(EnumType.STRING)
  @Column(name = "EVENT_TYPE")
  val eventType: EventType,

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
) : NomisAuditableEntity() {
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
