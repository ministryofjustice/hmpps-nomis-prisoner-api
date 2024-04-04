package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_CSIP_REPORTS")
@EntityOpen
data class CSIPReport(
  @Id
  @Column(name = "CSIP_ID")
  @SequenceGenerator(name = "CSIP_ID", sequenceName = "CSIP_ID", allocationSize = 1)
  @GeneratedValue(generator = "CSIP_ID")
  val id: Long = 0,

  @Column(name = "CSIP_SEQ")
  val sequence: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking,

  @Column(name = "ROOT_OFFENDER_ID", nullable = false)
  val rootOffenderId: Long,

  @Column(name = "RFR_INCIDENT_DATE", nullable = false)
  val incidentDateTime: LocalDateTime = LocalDateTime.now(),

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CSIPIncidentType.CSIP_TYP + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "RFR_INCIDENT_TYPE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val type: CSIPIncidentType,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CSIPIncidentLocation.CSIP_LOC + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "RFR_INCIDENT_LOCATION", referencedColumnName = "code", nullable = true)),
    ],
  )
  val location: CSIPIncidentLocation,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CSIPAreaOfWork.CSIP_FUNC + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "RFR_CSIP_FUNCTION", referencedColumnName = "code", nullable = true)),
    ],
  )
  val areaOfWork: CSIPAreaOfWork,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "RFR_REPORTED_BY")
  val reportedBy: Staff? = null,

  @Column(name = "RFR_DATE_REPORTED", nullable = false)
  val reportedDate: LocalDate = LocalDate.now(),

  @OneToMany(mappedBy = "csipReport", cascade = [CascadeType.ALL], orphanRemoval = true)
  var plans: MutableList<CSIPPlan> = mutableListOf(),

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
    other as CSIPReport

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id)"
  }
}
