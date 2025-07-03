package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_MOVEMENT_APPS")
class OffenderMovementApplication(
  @SequenceGenerator(name = "OFFENDER_MOVEMENT_APP_ID", sequenceName = "OFFENDER_MOVEMENT_APP_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_MOVEMENT_APP_ID")
  @Id
  @Column(name = "OFFENDER_MOVEMENT_APP_ID")
  val movementApplicationId: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "EVENT_CLASS")
  val eventClass: String = "EXT_MOV",

  @Column(name = "EVENT_TYPE")
  @Enumerated(EnumType.STRING)
  val eventType: EventType = EventType.TAP,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${MovementReason.MOVE_RSN}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "EVENT_SUB_TYPE", referencedColumnName = "code")),
    ],
  )
  val eventSubType: MovementReason,

  @Column(name = "APPLICATION_DATE")
  val applicationDate: LocalDate,

  @Column(name = "APPLICATION_TIME")
  val applicationTime: LocalDateTime,

  @Column(name = "FROM_DATE")
  val fromDate: LocalDate,

  @Column(name = "RELEASE_TIME")
  val releaseTime: LocalDateTime,

  @Column(name = "TO_DATE")
  val toDate: LocalDate,

  @Column(name = "RETURN_TIME")
  val returnTime: LocalDateTime,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${MovementApplicationStatus.MOV_APP_STAT}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "APPLICATION_STATUS", referencedColumnName = "code")),
    ],
  )
  val applicationStatus: MovementApplicationStatus,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
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

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${TemporaryAbsenceTransportType.TA_TRANSPORT}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "TRANSPORT_CODE", referencedColumnName = "code")),
    ],
  )
  val transportType: TemporaryAbsenceTransportType? = null,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @Column(name = "TO_ADDRESS_OWNER_CLASS")
  val toAddressOwnerClass: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_ADDRESS_ID")
  val toAddress: Address? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  val prison: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_AGY_LOC_ID")
  val toAgency: AgencyLocation? = null,

  @Column(name = "CONTACT_PERSON_NAME")
  val contactPersonName: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${MovementApplicationType.MOV_APP_TYPE}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "APPLICATION_TYPE", referencedColumnName = "code")),
    ],
  )
  val applicationType: MovementApplicationType,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${TemporaryAbsenceType.TAP_ABS_TYPE}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "TAP_ABS_TYPE", referencedColumnName = "code")),
    ],
  )
  val temporaryAbsenceType: TemporaryAbsenceType? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${TemporaryAbsenceSubType.TAP_ABS_STYP}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "TAP_ABS_SUBTYPE", referencedColumnName = "code")),
    ],
  )
  val temporaryAbsenceSubType: TemporaryAbsenceSubType? = null,

  @OneToOne(mappedBy = "temporaryAbsenceApplication", cascade = [CascadeType.ALL])
  var scheduledTemporaryAbsence: OffenderScheduledTemporaryAbsence? = null,

  @OneToMany(mappedBy = "offenderMovementApplication", cascade = [CascadeType.ALL])
  val movements: MutableList<OffenderMovementApplicationMulti> = mutableListOf(),
)
