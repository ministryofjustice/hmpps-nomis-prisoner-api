package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "OFFENDER_VISITS")
data class Visit(
  @SequenceGenerator(name = "OFFENDER_VISIT_ID", sequenceName = "OFFENDER_VISIT_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_VISIT_ID")
  @Id
  @Column(name = "OFFENDER_VISIT_ID", nullable = false)
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column
  var commentText: String? = null,

  @Column
  val visitorConcernText: String? = null,

  @Column(nullable = false)
  val visitDate: LocalDate? = null,

  @Column(nullable = false)
  val startTime: LocalDateTime? = null,

  @Column(nullable = false)
  val endTime: LocalDateTime? = null,

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + VisitType.VISIT_TYPE + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "VISIT_TYPE", referencedColumnName = "code", nullable = false))
    ]
  )
  val visitType: VisitType? = null,

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + VisitStatus.VISIT_STATUS + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "VISIT_STATUS",
          referencedColumnName = "code",
          nullable = false
        )
      )
    ]
  )
  var visitStatus: VisitStatus? = null,

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + SearchLevel.SEARCH_LEVEL + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "SEARCH_TYPE", referencedColumnName = "code", nullable = false))
    ]
  )

  val searchLevel: SearchLevel? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val location: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "VISIT_INTERNAL_LOCATION_ID")
  val agencyInternalLocation: AgencyInternalLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGENCY_VISIT_SLOT_ID")
  val agencyVisitSlot: AgencyVisitSlot? = null,

  @Column(name = "CLIENT_UNIQUE_REF")
  val vsipVisitId: String? = null,

  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  @JoinColumn(name = "OFFENDER_VISIT_ORDER_ID")
  var visitOrder: VisitOrder? = null,

  /* a list of all visitors including those without visitor orders */
  @OneToMany(mappedBy = "visit", cascade = [CascadeType.ALL])
  val visitors: MutableList<VisitVisitor> = mutableListOf(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Visit
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  // omit visitors to avoid recursion
  override fun toString(): String {
    return "Visit(id=$id, offenderBooking=$offenderBooking, commentText=$commentText, visitorConcernText=$visitorConcernText, visitDate=$visitDate, startTime=$startTime, endTime=$endTime, visitType=$visitType, visitStatus=$visitStatus, searchLevel=$searchLevel, location=$location, agencyInternalLocation=$agencyInternalLocation, vsipVisitId=$vsipVisitId)"
  }

  /* fields not used in production for info:

     RAISED_INCIDENT_NUMBER
     private Long raisedIncidentNumber;

     RAISED_INCIDENT_TYPE
     private IncidentType incidentType;

     OUTCOME_REASON_CODE
     private VisitOutcomeReason outcomeReason;

     "EVENT_OUTCOME" - not used since 2015
     private VisitOutcome outcome;

     "OVERRIDE_BAN_STAFF_ID" - not used since 2015
     private Long overrideBanStaffId;
  */
}
