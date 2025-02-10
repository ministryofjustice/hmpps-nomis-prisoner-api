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
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate
import java.time.LocalDateTime

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
  var visitDate: LocalDate,

  @Column(name = "START_TIME", nullable = false)
  var startDateTime: LocalDateTime,

  @Column(name = "END_TIME", nullable = false)
  var endDateTime: LocalDateTime,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + VisitType.VISIT_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "VISIT_TYPE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val visitType: VisitType,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + VisitStatus.VISIT_STATUS + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "VISIT_STATUS",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var visitStatus: VisitStatus,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + SearchLevel.SEARCH_LEVEL + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "SEARCH_TYPE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val searchLevel: SearchLevel? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val location: AgencyLocation,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "VISIT_INTERNAL_LOCATION_ID")
  var agencyInternalLocation: AgencyInternalLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGENCY_VISIT_SLOT_ID")
  var agencyVisitSlot: AgencyVisitSlot? = null,

  @ManyToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
  @JoinColumn(name = "OFFENDER_VISIT_ORDER_ID")
  var visitOrder: VisitOrder? = null,

  /* a list of all visitors including those without visitor orders */
  @OneToMany(mappedBy = "visit", cascade = [CascadeType.ALL], orphanRemoval = true)
  val visitors: MutableList<VisitVisitor> = mutableListOf(),

  @Column(name = "CREATE_DATETIME", nullable = false, insertable = false, updatable = false)
  var whenCreated: LocalDateTime = LocalDateTime.now(),

  @Column(name = "MODIFY_DATETIME", nullable = false, insertable = false, updatable = false)
  var whenUpdated: LocalDateTime? = null,

  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  val createUserId: String = "",

  @Column(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  val modifyUserId: String? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Visit
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  // omit visitors to avoid recursion
  override fun toString(): String = "Visit(id=$id, offenderBooking=$offenderBooking, commentText=$commentText, visitorConcernText=$visitorConcernText, visitDate=$visitDate, startDateTime=$startDateTime, endDateTime=$endDateTime, visitType=$visitType, visitStatus=$visitStatus, searchLevel=$searchLevel, location=$location, agencyInternalLocation=$agencyInternalLocation)"

  /* fields not used in production for info:

     @Column(name = "CLIENT_UNIQUE_REF")
     val vsipVisitId: String? = null,

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
