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
import java.time.LocalDate
import java.time.LocalDateTime

enum class EventClass {
  EXT_MOV, INT_MOV, COMM
}

@Entity
@Table(name = "OFFENDER_IND_SCHEDULES")
class OffenderIndividualSchedule(

  @Id
  @SequenceGenerator(name = "EVENT_ID", sequenceName = "EVENT_ID", allocationSize = 1)
  @GeneratedValue(generator = "EVENT_ID")
  @Column(name = "EVENT_ID", nullable = false)
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column
  val eventDate: LocalDate? = null,

  @Column
  val startTime: LocalDateTime? = null,

  @Column
  val endTime: LocalDateTime? = null,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val eventClass: EventClass = EventClass.INT_MOV, // INT_MOV only for now

  @Column(nullable = false)
  val eventType: String = "APP", // APP for appointment

  @ManyToOne(optional = false)
  @NotFound(action = NotFoundAction.EXCEPTION)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${EventSubType.INT_SCH_RSN}'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_SUB_TYPE", referencedColumnName = "code"))
    ]
  )
  val eventSubType: EventSubType,

  @ManyToOne(optional = false)
  @NotFound(action = NotFoundAction.EXCEPTION)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${EventStatus.EVENT_STS}'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_STATUS", referencedColumnName = "code"))
    ]
  )
  val eventStatus: EventStatus,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  val prison: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_INTERNAL_LOCATION_ID")
  val internalLocation: AgencyInternalLocation? = null,

) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderIndividualSchedule

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id, booking = ${offenderBooking.bookingId}, eventDate = $eventDate, startTime = $startTime)"
  }
}
