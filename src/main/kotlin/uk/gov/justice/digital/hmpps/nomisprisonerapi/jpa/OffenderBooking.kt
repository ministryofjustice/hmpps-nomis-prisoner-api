package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.Type
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
import javax.persistence.OneToOne
import javax.persistence.PrimaryKeyJoinColumn
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "OFFENDER_BOOKINGS")
@BatchSize(size = 25)
data class OffenderBooking(
  @SequenceGenerator(name = "OFFENDER_BOOK_ID", sequenceName = "OFFENDER_BOOK_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_BOOK_ID")
  @Id
  @Column(name = "OFFENDER_BOOK_ID")
  val bookingId: Long = 0,

  @Column(name = "BOOKING_SEQ", nullable = false)
  val bookingSequence: Int? = null,

  @Column(name = "BOOKING_NO")
  val bookNumber: String? = null,

  @Column(name = "BOOKING_TYPE")
  val bookingType: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_ID", nullable = false)
  val offender: Offender? = null,

  @Column(name = "DISCLOSURE_FLAG", nullable = false)
  val disclosureFlag: String = "Y",

  @Column(name = "BOOKING_BEGIN_DATE", nullable = false)
  val bookingBeginDate: LocalDateTime,

  //
  //    @OneToMany(mappedBy = "id.offenderBooking", cascade = CascadeType.ALL)
  //    @Default
  //    private List<OffenderProfileDetail> profileDetails = new ArrayList<>();
  //
  //    @OrderColumn(name = "MILITARY_SEQ")
  //    @ListIndexBase(1)
  //    @OneToMany(mappedBy = "bookingAndSequence.offenderBooking", cascade = CascadeType.ALL)
  //    private List<OffenderMilitaryRecord> militaryRecords;
  //
  //    @OrderColumn(name = "CASE_SEQ")
  //    @ListIndexBase(1)
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    private List<OffenderCourtCase> courtCases = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    @BatchSize(size = 25)
  //    private List<CourtOrder> courtOrders = new ArrayList<>();
  //
  //    @ListIndexBase(1)
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    private List<OffenderPropertyContainer> propertyContainers;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val location: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CREATE_AGY_LOC_ID")
  val createLocation: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "LIVING_UNIT_ID")
  val assignedLivingUnit: AgencyInternalLocation? = null,

  //    @ManyToOne(fetch = FetchType.LAZY)
  //    @JoinColumn(name = "ASSIGNED_STAFF_ID")
  //    private Staff assignedStaff;

  @Column(name = "AGENCY_IML_ID")
  val livingUnitMv: Long? = null,

  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Type(type = "yes_no")
  val active: Boolean = false,

  //    @OrderBy("effectiveDate ASC")
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    private List<OffenderNonAssociationDetail> nonAssociationDetails = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    @BatchSize(size = 25)
  //    private List<ExternalMovement> externalMovements = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    @BatchSize(size = 25)
  //    private List<OffenderImprisonmentStatus> imprisonmentStatuses = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    private List<OffenderCaseNote> caseNotes = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    private List<OffenderCharge> charges = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    @BatchSize(size = 25)
  //    private List<SentenceCalculation> sentenceCalculations = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    @Default
  //    @Exclude
  //    private List<KeyDateAdjustment> keyDateAdjustments = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    @Default
  //    @Exclude
  //    private List<SentenceAdjustment> sentenceAdjustments = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    @Default
  //    @Exclude
  //    @BatchSize(size = 25)
  //    private List<SentenceTerm> terms = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    @Default
  //    @Exclude
  //    @BatchSize(size = 25)
  //    private List<OffenderSentence> sentences = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    @Default
  //    @Exclude
  //    @BatchSize(size = 25)
  //    private List<OffenderImage> images = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    @Default
  //    @Exclude
  //    @BatchSize(size = 25)
  //    private List<OffenderAlert> alerts = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "offenderBooking", cascade = CascadeType.ALL)
  //    @Default
  //    @Exclude
  //    @BatchSize(size = 25)
  //    private List<OffenderIepLevel> iepLevels = new ArrayList<>();
  //
  //
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "ROOT_OFFENDER_ID", nullable = false)
  var rootOffender: Offender? = null,

  @Column(name = "BOOKING_STATUS")
  val bookingStatus: String? = null,

  @Column(name = "STATUS_REASON")
  val statusReason: String? = null,

  @Column(name = "COMMUNITY_ACTIVE_FLAG", nullable = false)
  val communityActiveFlag: String = "N",

  @Column(name = "SERVICE_FEE_FLAG", nullable = false)
  val serviceFeeFlag: String = "N",

  @Column(name = "COMM_STATUS")
  val commStatus: String? = null,

  @Column(name = "YOUTH_ADULT_CODE", nullable = false)
  val youthAdultCode: String? = null,

  @Column(name = "BOOKING_END_DATE")
  val bookingEndDate: LocalDateTime? = null,

  @Column(name = "IN_OUT_STATUS", nullable = false)
  val inOutStatus: String? = null,

  @Column(name = "ADMISSION_REASON")
  val admissionReason: String? = null,

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val visits: MutableList<Visit> = mutableListOf(),

  @OneToOne(mappedBy = "offenderBooking", cascade = [CascadeType.ALL])
  @PrimaryKeyJoinColumn
  var visitBalance: OffenderVisitBalance? = null,

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val visitBalanceAdjustments: MutableList<OffenderVisitBalanceAdjustment> = mutableListOf(),

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val contacts: MutableList<OffenderContactPerson> = mutableListOf(),

  //    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
  //    @PrimaryKeyJoinColumn
  //    private ReleaseDetail releaseDetail;
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderBooking
    return bookingId == other.bookingId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String {
    return javaClass.simpleName + "(" +
      "bookingId = " + bookingId + ", " +
      "bookNumber = " + bookNumber + ", " +
      "bookingSequence = " + bookingSequence + ", " +
      "active = " + active + ", " +
      "inOutStatus = " + inOutStatus + ")"
  }
}
