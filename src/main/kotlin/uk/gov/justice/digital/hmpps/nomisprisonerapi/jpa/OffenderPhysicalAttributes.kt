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
  var heightCentimetres: Int? = null,

  @Column(name = "HEIGHT_FT")
  var heightFeet: Int? = null,

  @Column(name = "HEIGHT_IN")
  var heightInches: Int? = null,

  @Column(name = "WEIGHT_KG")
  var weightKilograms: Int? = null,

  @Column(name = "WEIGHT_LBS")
  var weightPounds: Int? = null,
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
