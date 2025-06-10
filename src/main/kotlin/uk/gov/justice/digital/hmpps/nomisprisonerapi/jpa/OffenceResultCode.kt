package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.util.Objects

@Entity
@Table(name = "OFFENCE_RESULT_CODES")
@EntityOpen
class OffenceResultCode(
  @Id
  @Column(name = "RESULT_CODE")
  val code: String,

  val description: String,

  val dispositionCode: String,

  val chargeStatus: String,

  @Column(name = "CONVICTION_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  var conviction: Boolean = false,
) {

  companion object {
    const val RECALL_TO_PRISON = "1501"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenceResultCode
    return code == other.code
  }

  override fun hashCode(): Int = Objects.hashCode(code)
}
