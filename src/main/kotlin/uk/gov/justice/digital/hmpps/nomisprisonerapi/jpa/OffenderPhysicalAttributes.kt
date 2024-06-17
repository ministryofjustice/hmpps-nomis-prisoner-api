package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable
import java.time.LocalDateTime
import kotlin.math.roundToInt

@Embeddable
data class OffenderPhysicalAttributeId(
  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "ATTRIBUTE_SEQ", nullable = false)
  val sequence: Long,
) : Serializable

@Entity
@EntityOpen
@Table(name = "OFFENDER_PHYSICAL_ATTRIBUTES")
class OffenderPhysicalAttributes(
  @EmbeddedId
  val id: OffenderPhysicalAttributeId,

  @Column(name = "HEIGHT_CM")
  var heightCentimetres: Int?,

  @Column(name = "HEIGHT_FT")
  var heightFeet: Int?,

  @Column(name = "HEIGHT_IN")
  var heightInches: Int?,

  @Column(name = "WEIGHT_KG")
  var weightKilograms: Int?,

  @Column(name = "WEIGHT_LBS")
  var weightPounds: Int?,
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

  fun getHeightInCentimetres() =
    // Take height in cm if it exists because the data is more accurate (being a smaller unit than inches)
    if (heightCentimetres != null) {
      heightCentimetres
    } else {
      heightFeet?.let { ((it * 12) + (heightInches ?: 0)) * 2.54 }?.roundToInt()
    }

  fun getWeightInKilograms() =
    // Take weight in lb and convert if it exists because the data is more accurate (being a smaller unit than kg). See the unit tests for an example explaining why.
    if (weightPounds != null) {
      weightPounds!!.let { (it * 0.453592) }.roundToInt()
    } else {
      weightKilograms
    }
}
