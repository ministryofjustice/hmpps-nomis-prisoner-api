package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate

@Suppress("JpaEntityWithValAttributesInspection")
@Entity
@Table(name = "IMPRISONMENT_STATUSES")
@EntityOpen
class ImprisonmentStatus(

  @Id
  @Column(name = "IMPRISONMENT_STATUS_ID")
  val id: Long,

  @Column(name = "IMPRISONMENT_STATUS")
  val code: String,

  @Column(name = "DESCRIPTION")
  val description: String,

  @Column(name = "BAND_CODE")
  val bandCode: String,

  @Column(name = "RANK_VALUE")
  val rank: Long,

  @Convert(converter = YesNoConverter::class)
  @Column(name = "ACTIVE_FLAG", nullable = false)
  val active: Boolean = false,

  @Column(name = "EXPIRY_DATE")
  var expiryDate: LocalDate? = null,

  @Column(name = "IMPRISONMENT_STATUS_SEQ")
  val sequence: Long?,
) : NomisAuditableEntityBasic() {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ImprisonmentStatus

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id, code = $code )"
}
