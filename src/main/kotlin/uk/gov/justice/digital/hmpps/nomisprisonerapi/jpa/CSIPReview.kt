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
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_CSIP_REVIEWS")
@EntityOpen
class CSIPReview(
  @Id
  @Column(name = "REVIEW_ID")
  @SequenceGenerator(name = "REVIEW_ID", sequenceName = "REVIEW_ID", allocationSize = 1)
  @GeneratedValue(generator = "REVIEW_ID")
  override val id: Long = 0,

  @Column(name = "REVIEW_SEQ", nullable = false)
  var reviewSequence: Int,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CSIP_ID")
  val csipReport: CSIPReport,

  @OneToMany(mappedBy = "csipReview", cascade = [CascadeType.ALL], orphanRemoval = true)
  val attendees: MutableList<CSIPAttendee> = mutableListOf(),

  @Column(name = "REMAIN_ON_CSIP")
  @Convert(converter = YesNoConverter::class)
  var remainOnCSIP: Boolean = false,

  @Column(name = "CSIP_UPDATED")
  @Convert(converter = YesNoConverter::class)
  var csipUpdated: Boolean = false,

  @Column(name = "CASE_NOTE")
  @Convert(converter = YesNoConverter::class)
  var caseNote: Boolean = false,

  @Column(name = "CLOSE_CSIP")
  @Convert(converter = YesNoConverter::class)
  var closeCSIP: Boolean = false,

  @Column(name = "PEOPLE_INFORMED")
  @Convert(converter = YesNoConverter::class)
  var peopleInformed: Boolean = false,

  @Column(name = "SUMMARY")
  var summary: String?,

  @Column(name = "NEXT_REVIEW_DATE", nullable = false)
  var nextReviewDate: LocalDate?,

  @Column(name = "CLOSE_DATE")
  var closeDate: LocalDate?,

  @Column(name = "CREATE_DATE")
  val recordedDate: LocalDate? = LocalDate.now(),
  @Column(name = "CREATE_USER")
  val recordedUser: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CREATE_USER", insertable = false, updatable = false)
  val recordedByStaffUserAccount: StaffUserAccount? = null,

  @Column
  var auditModuleName: String? = null,

  @Column(name = "MODIFY_USER_ID", insertable = false, updatable = false)
  @Generated
  var lastModifiedUsername: String? = null,

  @Column(name = "MODIFY_DATETIME", insertable = false, updatable = false)
  @Generated
  var lastModifiedDateTime: LocalDateTime? = null,

  // ---- NOT MAPPED columns ---- //
  // All AUDIT data except auditModuleName
) : CSIPChild {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDateTime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CSIPReview

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(id = $id)"
}
