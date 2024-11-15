package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.time.LocalDateTime

@Entity
@DiscriminatorValue("OFF_IDM")
class OffenderIdentifyingMarkImage(
  id: Long,
  captureDateTime: LocalDateTime,
  fullSizeImage: ByteArray,
  thumbnailImage: ByteArray,
  imageObjectSequence: Long? = null,
  active: Boolean,
  imageSource: ImageSource,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns(
    JoinColumn(name = "OFFENDER_BOOK_ID", referencedColumnName = "OFFENDER_BOOK_ID", nullable = false),
    JoinColumn(name = "IMAGE_OBJECT_ID", referencedColumnName = "ID_MARK_SEQ", nullable = false),
  )
  val identifyingMark: OffenderIdentifyingMark,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + BodyPart.BODY_PART_CODE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "ORIENTATION_TYPE", referencedColumnName = "code", nullable = true, updatable = false, insertable = false)),
    ],
  )
  val orientationType: BodyPart,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + MarkType.MARK_TYPE_CODE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "IMAGE_VIEW_TYPE", referencedColumnName = "code", nullable = true, updatable = false, insertable = false)),
    ],
  )
  val imageViewType: MarkType,
) : OffenderImage(
  id,
  captureDateTime,
  fullSizeImage,
  thumbnailImage,
  imageObjectSequence,
  active,
  imageSource,
  orientationType.code,
  imageViewType.code,
)
