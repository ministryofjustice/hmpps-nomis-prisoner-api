package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

@EntityOpen
@Entity(name = "OFFENDER_IMAGES")
@DiscriminatorColumn(name = "IMAGE_OBJECT_TYPE", discriminatorType = DiscriminatorType.STRING)
@Inheritance
abstract class OffenderImage(
  @Id
  @SequenceGenerator(name = "OFFENDER_IMAGE_ID", sequenceName = "OFFENDER_IMAGE_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_IMAGE_ID")
  @Column(name = "OFFENDER_IMAGE_ID")
  val id: Long = 0,

  @Column(name = "CAPTURE_DATETIME")
  val captureDateTime: LocalDateTime,

  @Column(name = "FULL_SIZE_IMAGE")
  @Lob
  val fullSizeImage: ByteArray,

  @Column(name = "THUMBNAIL_IMAGE")
  @Lob
  val thumbnailImage: ByteArray,

  @Column(name = "IMAGE_OBJECT_SEQ")
  val imageObjectSequence: Long? = null,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + ImageSource.IMAGE_SOURCE_CODE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "IMAGE_SOURCE_CODE", referencedColumnName = "code")),
    ],
  )
  val imageSource: ImageSource,

  @Column(name = "ORIENTATION_TYPE")
  val orientationTypeCode: String,

  @Column(name = "IMAGE_VIEW_TYPE")
  val imageViewTypeCode: String,
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
