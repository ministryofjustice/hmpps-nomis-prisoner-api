package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Inheritance
import org.hibernate.Hibernate
import org.hibernate.type.YesNoConverter
import java.io.Serializable
import java.time.LocalDate
import java.util.Objects

@Entity(name = "REFERENCE_CODES")
@org.hibernate.annotations.DiscriminatorFormula(value = "domain")
@Inheritance
abstract class ReferenceCode(

  @Column(insertable = false, updatable = false)
  open val domain: String,

  @Column(insertable = false, updatable = false)
  open val code: String,

  @EmbeddedId
  open val id: Pk,

  open var description: String,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  open var active: Boolean = true,

  @Column(name = "LIST_SEQ")
  open var sequence: Int? = 0,

  open var parentCode: String? = null,

  open var expiredDate: LocalDate? = null,

) : Serializable {
  constructor(domain: String, code: String, description: String) : this(
    domain = domain,
    code = code,
    id = Pk(domain, code),
    description = description,
  )

  constructor(domain: String, code: String, description: String, active: Boolean, sequence: Int?, parentCode: String?, expiredDate: LocalDate?) : this(
    domain = domain,
    code = code,
    id = Pk(domain, code),
    description = description,
    active = active,
    sequence = sequence,
    parentCode = parentCode,
    expiredDate = expiredDate,
  )

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

  @Embeddable
  data class Pk(
    @Column(name = "DOMAIN", nullable = false)
    val domain: String,
    @Column(name = "CODE", nullable = false)
    val code: String,
  ) : Serializable
}
