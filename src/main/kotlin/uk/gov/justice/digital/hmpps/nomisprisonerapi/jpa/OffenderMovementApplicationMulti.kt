package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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
@Table(name = "OFFENDER_MOVEMENT_APPS_MULTI")
class OffenderMovementApplicationMulti(
  @SequenceGenerator(name = "OFF_MOVEMENT_APPS_MULTI_ID", sequenceName = "OFF_MOVEMENT_APPS_MULTI_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFF_MOVEMENT_APPS_MULTI_ID")
  @Id
  @Column(name = "OFF_MOVEMENT_APPS_MULTI_ID")
  val movementApplicationMultiId: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_MOVEMENT_APP_ID", nullable = false)
  val offenderMovementApplication: OffenderMovementApplication,

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

  @Column(name = "FROM_DATE")
  val fromDate: LocalDate,

  @Column(name = "RELEASE_TIME")
  val releaseTime: LocalDateTime,

  @Column(name = "TO_DATE")
  val toDate: LocalDate,

  @Column(name = "RETURN_TIME")
  val returnTime: LocalDateTime,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @Column(name = "TO_ADDRESS_OWNER_CLASS")
  val toAddressOwnerClass: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_ADDRESS_ID")
  val toAddress: Address? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_AGY_LOC_ID")
  val toAgency: AgencyLocation? = null,

  @Column(name = "CONTACT_PERSON_NAME")
  val contactPersonName: String? = null,
) : NomisAuditableEntity()
