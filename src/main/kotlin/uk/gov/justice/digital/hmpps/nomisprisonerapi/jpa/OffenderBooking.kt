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
  val offender: Offender,

  @Column(name = "DISCLOSURE_FLAG", nullable = false)
  val disclosureFlag: String = "Y",

  @Column(name = "BOOKING_BEGIN_DATE", nullable = false)
  val bookingBeginDate: LocalDateTime,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val location: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CREATE_AGY_LOC_ID")
  val createLocation: AgencyLocation? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "LIVING_UNIT_ID")
  val assignedLivingUnit: AgencyInternalLocation? = null,

  @Column(name = "AGENCY_IML_ID")
  val livingUnitMv: Long? = null,

  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Type(type = "yes_no")
  val active: Boolean = false,

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

  @OneToMany(mappedBy = "id.offenderBooking", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val incentives: MutableList<Incentive> = mutableListOf(),
) {
  fun getNextSequence(): Long {
    val i = incentives.stream().max(Comparator.comparing { (a, _) -> a.sequence })
    if (i.isPresent) {
      return i.get().id.sequence + 1
    } else {
      return 1
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderBooking
    return bookingId == other.bookingId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String =
    javaClass.simpleName + "(" +
      "bookingId = " + bookingId + ", " +
      "bookNumber = " + bookNumber + ", " +
      "bookingSequence = " + bookingSequence + ", " +
      "active = " + active + ", " +
      "inOutStatus = " + inOutStatus + ")"
}
