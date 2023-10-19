package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.ColumnResult
import jakarta.persistence.ConstructorResult
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedNativeQuery
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.SqlResultSetMapping
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing.AdjustmentIdResponse
import java.time.LocalDate
import java.time.LocalDateTime

const val adjustmentIdsInner = "select adjustment_id, adjustment_category, create_datetime  from (" +
  "    select offender_key_date_adjust_id adjustment_id, 'KEY-DATE' adjustment_category, create_datetime from offender_key_date_adjusts " +
  "    union " +
  "    select  offender_sentence_adjust_id adjustment_id, 'SENTENCE' adjustment_category , create_datetime from offender_sentence_adjusts " +
  "    where offender_key_date_adjust_id is null" +
  "    )" +
  "    where (:fromDate is null or create_datetime >= :fromDate) and (:toDate is null or create_datetime < :toDate)"

@NamedNativeQuery(
  name = "OffenderKeyDateAdjustment.adjustmentIdsQuery_named",
  query = "$adjustmentIdsInner order by create_datetime",
  resultSetMapping = "adjustmentIdsMapping",
)
@NamedNativeQuery(
  name = "OffenderKeyDateAdjustment.adjustmentIdsQuery_named.count",
  query = "select count(*) cresult  from ($adjustmentIdsInner)",
  resultSetMapping = "adjustmentIdsMapping.count",
)
@SqlResultSetMapping(
  name = "adjustmentIdsMapping",
  classes = [
    ConstructorResult(
      targetClass = AdjustmentIdResponse::class,
      columns = arrayOf(
        ColumnResult(name = "adjustment_id"),
        ColumnResult(name = "adjustment_category"),
      ),
    ),
  ],
)
@SqlResultSetMapping(name = "adjustmentIdsMapping.count", columns = arrayOf(ColumnResult(name = "cresult")))
@Entity
@Table(name = "OFFENDER_KEY_DATE_ADJUSTS")
class OffenderKeyDateAdjustment(
  @SequenceGenerator(
    name = "OFFENDER_KEY_DATE_ADJUST_ID",
    sequenceName = "OFFENDER_KEY_DATE_ADJUST_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "OFFENDER_KEY_DATE_ADJUST_ID")
  @Id
  @Column(name = "OFFENDER_KEY_DATE_ADJUST_ID", nullable = false)
  val id: Long = 0,

  @ManyToOne
  @JoinColumn(name = "SENTENCE_ADJUST_CODE", nullable = false)
  var sentenceAdjustment: SentenceAdjustment,

  @Column(name = "ADJUST_DATE")
  var adjustmentDate: LocalDate?,

  @Column(name = "ADJUST_DAYS")
  var adjustmentNumberOfDays: Long,

  @Column(name = "ADJUST_FROM_DATE")
  var fromDate: LocalDate? = null,

  @Column(name = "ADJUST_TO_DATE")
  var toDate: LocalDate? = null,

  @Column(name = "COMMENT_TEXT")
  var comment: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  var active: Boolean = false,

  @Column(name = "CREATE_DATETIME")
  val createdDate: LocalDateTime = LocalDateTime.now(),
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderKeyDateAdjustment
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
