package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.YesNoConverter
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_BOOKINGS")
data class OffenderBooking(
  @SequenceGenerator(name = "OFFENDER_BOOK_ID", sequenceName = "OFFENDER_BOOK_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_BOOK_ID")
  @Id
  @Column(name = "OFFENDER_BOOK_ID")
  val bookingId: Long = 0,

  @Column(name = "BOOKING_SEQ", nullable = false)
  val bookingSequence: Int = 1,

  @Column(name = "BOOKING_NO")
  val bookNumber: String? = null,

  @Column(name = "BOOKING_TYPE")
  val bookingType: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_ID", nullable = false)
  val offender: Offender,

  @Column(name = "DISCLOSURE_FLAG", nullable = false)
  val disclosureFlag: String = "Y",

  @Column(name = "BOOKING_BEGIN_DATE", nullable = false)
  val bookingBeginDate: LocalDateTime,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID")
  var location: AgencyLocation? = null,
  // Annoyingly there are just a handful of rows where AGY_LOC_ID is null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CREATE_AGY_LOC_ID")
  val createLocation: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "LIVING_UNIT_ID")
  val assignedLivingUnit: AgencyInternalLocation? = null,

  @Column(name = "AGENCY_IML_ID")
  val livingUnitMv: Long? = null,

  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  var active: Boolean = false,

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
  var bookingEndDate: LocalDateTime? = null,

  @Column(name = "IN_OUT_STATUS", nullable = false)
  var inOutStatus: String? = null,

  @Column(name = "ADMISSION_REASON")
  val admissionReason: String? = null,

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val visits: MutableList<Visit> = mutableListOf(),

  @OneToOne(mappedBy = "offenderBooking", cascade = [CascadeType.ALL])
  @PrimaryKeyJoinColumn
  var visitBalance: OffenderVisitBalance? = null,

  @OneToOne(mappedBy = "offenderBooking", cascade = [CascadeType.ALL])
  @PrimaryKeyJoinColumn
  var fixedTermRecall: OffenderFixedTermRecall? = null,

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val visitBalanceAdjustments: List<OffenderVisitBalanceAdjustment> = listOf(),

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val contacts: MutableList<OffenderContactPerson> = mutableListOf(),

  @OneToMany(mappedBy = "id.offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val incentives: MutableList<Incentive> = mutableListOf(),

  @OneToMany(mappedBy = "id.offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val externalMovements: MutableList<OffenderExternalMovement> = mutableListOf(),

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val offenderProgramProfiles: MutableList<OffenderProgramProfile> = mutableListOf(),

  @OneToMany(mappedBy = "id.offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val sentences: MutableList<OffenderSentence> = mutableListOf(),

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val keyDateAdjustments: MutableList<OffenderKeyDateAdjustment> = mutableListOf(),

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val offenderAppointments: MutableList<OffenderAppointment> = mutableListOf(),

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @SQLRestriction("OIC_INCIDENT_ID is not null")
  val adjudicationParties: MutableList<AdjudicationIncidentParty> = mutableListOf(),

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val courtCases: MutableList<CourtCase> = mutableListOf(),

  @OneToMany(mappedBy = "id.offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val alerts: MutableList<OffenderAlert> = mutableListOf(),

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val documents: MutableList<IWPDocument> = mutableListOf(),

  @OneToMany(mappedBy = "id.offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val physicalAttributes: MutableList<OffenderPhysicalAttributes> = mutableListOf(),

  @OneToMany(mappedBy = "id.offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val profiles: MutableList<OffenderProfile> = mutableListOf(),

  @OneToMany(mappedBy = "id.offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val profileDetails: MutableList<OffenderProfileDetail> = mutableListOf(),

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val images: MutableList<OffenderBookingImage> = mutableListOf(),

  @OneToMany(mappedBy = "id.offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val identifyingMarks: MutableList<OffenderIdentifyingMark> = mutableListOf(),

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val restrictions: MutableList<OffenderRestrictions> = mutableListOf(),

  @OneToMany(mappedBy = "offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val temporaryAbsenceApplications: MutableList<OffenderMovementApplication> = mutableListOf(),
) {
  fun getNextSequence(): Long = incentives.maxOfOrNull { it.id.sequence }?.let { it + 1 } ?: 1

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderBooking
    return bookingId == other.bookingId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = javaClass.simpleName + "(" +
    "bookingId = " + bookingId + ", " +
    "bookNumber = " + bookNumber + ", " +
    "bookingSequence = " + bookingSequence + ", " +
    "active = " + active + ", " +
    "inOutStatus = " + inOutStatus + ")"
}

fun OffenderBooking.hasBeenReleased() = !this.active && this.inOutStatus == "OUT"
fun OffenderBooking.status() = "${if (this.active) "ACTIVE" else "INACTIVE"} $inOutStatus"
fun OffenderBooking.maxMovementSequence() = this.externalMovements.maxByOrNull { it.id.sequence }?.id?.sequence ?: 0
fun OffenderBooking.activeExternalMovement() = this.externalMovements.filter { it.active }
