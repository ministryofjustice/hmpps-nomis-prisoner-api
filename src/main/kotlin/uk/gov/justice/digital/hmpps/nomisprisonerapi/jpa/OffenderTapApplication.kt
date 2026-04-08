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
import jakarta.persistence.NamedAttributeNode
import jakarta.persistence.NamedEntityGraph
import jakarta.persistence.NamedSubgraph
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@EntityOpen
@Entity
@Table(name = "OFFENDER_MOVEMENT_APPS")
@NamedEntityGraph(
  name = "tap-application",
  attributeNodes = [
    NamedAttributeNode(value = "offenderBooking"),
    NamedAttributeNode(value = "tapScheduleOuts", subgraph = "tap-schedule-out"),
    NamedAttributeNode(value = "eventSubType"),
    NamedAttributeNode(value = "applicationStatus"),
    NamedAttributeNode(value = "escort"),
    NamedAttributeNode(value = "transportType"),
    NamedAttributeNode(value = "applicationType"),
    NamedAttributeNode(value = "tapType"),
    NamedAttributeNode(value = "tapSubType"),
  ],
  subgraphs = [
    NamedSubgraph(
      name = "tap-schedule-out",
      attributeNodes = [
        NamedAttributeNode(value = "tapMovementOut", subgraph = "tap-movement"),
        NamedAttributeNode(value = "transportType"),
        NamedAttributeNode(value = "escort"),
        NamedAttributeNode(value = "eventStatus"),
        NamedAttributeNode(value = "eventSubType"),
      ],
    ),
    NamedSubgraph(
      name = "tap-movement",
      attributeNodes = [
        NamedAttributeNode(value = "movementType"),
        NamedAttributeNode(value = "movementReason"),
        NamedAttributeNode(value = "arrestAgency"),
        NamedAttributeNode(value = "escort"),
        NamedAttributeNode(value = "fromCity"),
        NamedAttributeNode(value = "toCity"),
      ],
    ),
  ],
)
@NamedEntityGraph(
  name = "tap-application-only",
  attributeNodes = [
    NamedAttributeNode(value = "offenderBooking"),
    NamedAttributeNode(value = "eventSubType"),
    NamedAttributeNode(value = "applicationStatus"),
    NamedAttributeNode(value = "escort"),
    NamedAttributeNode(value = "transportType"),
    NamedAttributeNode(value = "applicationType"),
    NamedAttributeNode(value = "tapType"),
    NamedAttributeNode(value = "tapSubType"),
    NamedAttributeNode(value = "toAddress"),
  ],
)
class OffenderTapApplication(
  @SequenceGenerator(name = "OFFENDER_MOVEMENT_APP_ID", sequenceName = "OFFENDER_MOVEMENT_APP_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_MOVEMENT_APP_ID")
  @Id
  @Column(name = "OFFENDER_MOVEMENT_APP_ID")
  val tapApplicationId: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "EVENT_CLASS")
  val eventClass: String = "EXT_MOV",

  @Column(name = "EVENT_TYPE")
  @Enumerated(EnumType.STRING)
  val eventType: EventType = EventType.TAP,

  @ManyToOne
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
  var eventSubType: MovementReason,

  @Column(name = "APPLICATION_DATE")
  val applicationDate: LocalDateTime,

  @Column(name = "APPLICATION_TIME")
  val applicationTime: LocalDateTime,

  @Column(name = "FROM_DATE")
  var fromDate: LocalDate,

  @Column(name = "RELEASE_TIME")
  var releaseTime: LocalDateTime,

  @Column(name = "TO_DATE")
  var toDate: LocalDate,

  @Column(name = "RETURN_TIME")
  var returnTime: LocalDateTime,

  @ManyToOne
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
  var applicationStatus: MovementApplicationStatus,

  @ManyToOne
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
  var escort: Escort? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${TapTransportType.TA_TRANSPORT}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "TRANSPORT_CODE", referencedColumnName = "code")),
    ],
  )
  var transportType: TapTransportType? = null,

  @Column(name = "COMMENT_TEXT")
  var comment: String? = null,

  @Column(name = "TO_ADDRESS_OWNER_CLASS")
  var toAddressOwnerClass: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_ADDRESS_ID")
  @NotFound(action = NotFoundAction.IGNORE)
  var toAddress: Address? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  val prison: AgencyLocation,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TO_AGY_LOC_ID")
  var toAgency: AgencyLocation? = null,

  @Column(name = "CONTACT_PERSON_NAME")
  var contactPersonName: String? = null,

  @ManyToOne
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
  var applicationType: MovementApplicationType,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${TapType.TAP_ABS_TYPE}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "TAP_ABS_TYPE", referencedColumnName = "code")),
    ],
  )
  var tapType: TapType? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${TapSubType.TAP_ABS_STYP}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "TAP_ABS_SUBTYPE", referencedColumnName = "code")),
    ],
  )
  var tapSubType: TapSubType? = null,

  @OneToMany(mappedBy = "tapApplication", cascade = [CascadeType.ALL], orphanRemoval = true)
  var tapScheduleOuts: MutableList<OffenderTapScheduleOut> = mutableListOf(),
) : NomisAuditableEntityBasic() {

  fun isApproved(): Boolean = applicationStatus.code in listOf("APP-SCH", "APP-UNSCH")
  fun isUnapproved(): Boolean = !isApproved()
  fun isSingle(): Boolean = applicationType.code == "SINGLE"
  fun hasSchedules(): Boolean = tapScheduleOuts.isNotEmpty()
}
