package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressUsage.PK
import java.io.Serializable

@Entity
@Table(name = "ADDRESS_USAGES")
@IdClass(PK::class)
data class AddressUsage(

  @Id
  val id: Long,

  @Id
  val addressUsage: String,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = false,

  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AddressUsageType.ADDRESS_TYPE + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "ADDRESS_USAGE", referencedColumnName = "code"))
    ]
  )
  val addressUsageType: AddressUsageType
) {
  data class PK(
    @Column(name = "ADDRESS_ID", updatable = false, insertable = false)
    val id: Long,

    @Column(name = "ADDRESS_USAGE", updatable = false, insertable = false)
    val addressUsage: String
  ) : Serializable
}
