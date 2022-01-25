package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import java.io.Serializable
import java.util.Objects
import javax.persistence.Column
import javax.persistence.DiscriminatorColumn
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.Inheritance

@Entity(name = "REFERENCE_CODES")
@DiscriminatorColumn(name = "domain")
@Inheritance
@IdClass(ReferenceCode.Pk::class)
abstract class ReferenceCode(

  @Id
  @Column(insertable = false, updatable = false)
  open val domain: String? = null,

  @Id
  open val code: String? = null,

  open val description: String? = null

) : Serializable {

  data class Pk(
    val domain: String? = null,
    val code: String? = null
  ) : Serializable {
    constructor() : this(null, null)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ReferenceCode
    return if (domain != other.domain) false else code == other.code
  }

  override fun hashCode(): Int {
    var result = Objects.hashCode(domain)
    result = 31 * result + Objects.hashCode(code)
    return result
  }

  companion object {
    fun getDescriptionOrNull(referenceCode: ReferenceCode?): String? {
      return referenceCode?.description
    }

    fun getCodeOrNull(referenceCode: ReferenceCode?): String? {
      return referenceCode?.code
    }
  }
}
