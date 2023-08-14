package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import java.time.LocalDate

@Entity
@Table(name = "AGY_INC_INVESTIGATIONS")
class AdjudicationInvestigation(
  @SequenceGenerator(
    name = "AGY_INC_INVESTIGATION_ID",
    sequenceName = "AGY_INC_INVESTIGATION_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "AGY_INC_INVESTIGATION_ID")
  @Id
  @Column(name = "AGY_INC_INVESTIGATION_ID")
  val id: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "INVESTIGATOR_ID")
  val investigator: Staff,

  @Column
  val assignedDate: LocalDate = LocalDate.now(),

  @Column(name = "COMMENT_TEXT")
  val comment: String?,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "AGENCY_INCIDENT_ID",
        referencedColumnName = "AGENCY_INCIDENT_ID",
      ),
      JoinColumn(
        name = "PARTY_SEQ",
        referencedColumnName = "PARTY_SEQ",
      ),
    ],
  )
  val incidentParty: AdjudicationIncidentParty,

  @OneToMany(mappedBy = "investigation", cascade = [CascadeType.ALL], orphanRemoval = true)
  val evidence: MutableList<AdjudicationEvidence> = mutableListOf(),
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  lateinit var createUsername: String

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationInvestigation
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
