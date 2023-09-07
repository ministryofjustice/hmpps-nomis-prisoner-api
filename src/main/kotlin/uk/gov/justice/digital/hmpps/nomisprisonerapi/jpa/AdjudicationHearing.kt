package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OIC_HEARINGS")
@NamedEntityGraph(
  name = "full-hearing",
  attributeNodes = [
    NamedAttributeNode(value = "agencyInternalLocation"),
    NamedAttributeNode(value = "hearingStaff", subgraph = "staff-accounts"),
    NamedAttributeNode(value = "hearingParty"),
    NamedAttributeNode(value = "hearingType"),
    NamedAttributeNode(value = "eventStatus"),
  ],
  subgraphs = [
    NamedSubgraph(name = "staff-accounts", attributeNodes = [NamedAttributeNode("accounts")]),
  ],
)
class AdjudicationHearing(
  @SequenceGenerator(
    name = "OIC_HEARING_ID",
    sequenceName = "OIC_HEARING_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "OIC_HEARING_ID")
  @Id
  @Column(name = "OIC_HEARING_ID")
  val id: Long = 0,

  @Column(name = "OIC_INCIDENT_ID")
  val adjudicationNumber: Long,

  @Column(name = "HEARING_TIME")
  var hearingDateTime: LocalDateTime? = LocalDateTime.now(),

  @Column(name = "HEARING_DATE")
  var hearingDate: LocalDate? = LocalDate.now(),

  @Column(name = "SCHEDULE_TIME")
  val scheduleDateTime: LocalDateTime? = LocalDateTime.now(),

  @Column(name = "SCHEDULE_DATE")
  val scheduleDate: LocalDate? = LocalDate.now(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name = "OIC_INCIDENT_ID",
    referencedColumnName = "OIC_INCIDENT_ID",
    nullable = false,
    insertable = false,
    updatable = false,
  )
  val hearingParty: AdjudicationIncidentParty,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "HEARING_STAFF_ID")
  val hearingStaff: Staff? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "INTERNAL_LOCATION_ID")
  var agencyInternalLocation: AgencyInternalLocation?,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AdjudicationHearingType.OIC_HEAR + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "OIC_HEARING_TYPE", referencedColumnName = "code")),
    ],
  )
  var hearingType: AdjudicationHearingType?,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @Column(name = "REPRESENTATIVE_TEXT")
  val representativeText: String? = null,

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
  var eventStatus: EventStatus? = null,

  @Column
  val eventId: Long? = null,

  @OneToMany(mappedBy = "hearing", cascade = [CascadeType.ALL], orphanRemoval = true)
  val hearingResults: MutableList<AdjudicationHearingResult> = mutableListOf(),

  @Column(name = "CREATE_DATETIME", nullable = false)
  var whenCreated: LocalDateTime = LocalDateTime.now(),
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationHearing
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
