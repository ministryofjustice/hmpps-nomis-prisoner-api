package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.time.LocalDateTime

@Entity
@DiscriminatorValue("OFF_IDM")
class OffenderImageIdentifyingMark(
  id: Long,
  offenderBooking: OffenderBooking,
  captureDateTime: LocalDateTime,
  fullSizeImage: ByteArray,
  thumbnailImage: ByteArray,
  imageObjectId: Long? = null,
  imageObjectSequence: Long? = null,
  active: Boolean,
  imageSource: ImageSource,

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
  offenderBooking,
  captureDateTime,
  fullSizeImage,
  thumbnailImage,
  imageObjectId,
  imageObjectSequence,
  active,
  imageSource,
  orientationType.code,
  imageViewType.code,
)
