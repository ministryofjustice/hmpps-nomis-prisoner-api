package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

@Embeddable
class OffenderIdentifyingMarkId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking,

  @Column(name = "ID_MARK_SEQ")
  val idMarkSequence: Long,
)

@EntityOpen
@Entity(name = "OFFENDER_IDENTIFYING_MARKS")
class OffenderIdentifyingMark(
  @EmbeddedId
  val id: OffenderIdentifyingMarkId,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + BodyPart.BODY_PART_CODE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "BODY_PART_CODE", referencedColumnName = "code")),
    ],
  )
  val bodyPart: BodyPart,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + MarkType.MARK_TYPE_CODE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "MARK_TYPE", referencedColumnName = "code")),
    ],
  )
  val markType: MarkType,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + Side.SIDE_CODE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "SIDE_CODE", referencedColumnName = "code")),
    ],
  )
  val side: Side,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + PartOrientation.PART_ORIENTATION_CODE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "PART_ORIENTATION_CODE", referencedColumnName = "code")),
    ],
  )
  val partOrientation: PartOrientation,

  @Column(name = "COMMENT_TEXT")
  val commentText: String? = null,

  @OneToMany(mappedBy = "identifyingMark", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val images: MutableList<OffenderIdentifyingMarkImage> = mutableListOf(),
) {

  @Column(name = "CREATE_DATETIME")
  @Generated
  lateinit var createDatetime: LocalDateTime

  @Column(name = "CREATE_USER_ID")
  @Generated
  lateinit var createUserId: String

  @Column(name = "MODIFY_DATETIME")
  @Generated
  var modifyDatetime: LocalDateTime? = null

  @Column(name = "MODIFY_USER_ID")
  @Generated
  var modifyUserId: String? = null

  @Column(name = "AUDIT_MODULE_NAME")
  @Generated
  var auditModuleName: String? = null
}
