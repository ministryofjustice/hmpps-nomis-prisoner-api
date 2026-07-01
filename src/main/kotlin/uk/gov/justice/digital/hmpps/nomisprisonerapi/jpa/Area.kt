package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate

@DiscriminatorColumn(name = "AREA_CLASS")
@Inheritance
@Entity
@Table(name = "AREAS")
@EntityOpen
class AbstractArea(
  @Id
  @Column(name = "AREA_CODE")
  var areaCode: String,

  @Column(name = "DESCRIPTION")
  var description: String,

  // could map as an Area
  @Column(name = "PARENT_AREA_CODE")
  var parentAreaCode: String? = null,

  @Column(name = "LIST_SEQ")
  var listSequence: Int,

  @Convert(converter = YesNoConverter::class)
  @Column(name = "ACTIVE_FLAG", nullable = false)
  var active: Boolean,

  @Column(name = "EXPIRY_DATE")
  var expiryDate: LocalDate?,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + TypeOfArea.AREA_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "AREA_TYPE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var type: TypeOfArea?,

) : NomisAuditableEntityBasic() {

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AreaClass.AREA_CLASS + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "AREA_CLASS",
          referencedColumnName = "code",
          nullable = true,
          updatable = false,
          insertable = false,
        ),
      ),
    ],
  )
  lateinit var areaClass: AreaClass

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AbstractArea

    return areaCode == other.areaCode
  }

  override fun hashCode(): Int = areaCode.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $areaCode )"
}

@Entity
@DiscriminatorValue(Area.AREA_TYPE)
class Area(
  areaCode: String,
  description: String,
  listSequence: Int,
  parentAreaCode: String?,
  active: Boolean,
  expiryDate: LocalDate?,
  type: TypeOfArea?,
) : AbstractArea(
  areaCode = areaCode,
  description = description,
  parentAreaCode = parentAreaCode,
  listSequence = listSequence,
  active = active,
  expiryDate = expiryDate,
  type = type,
) {
  companion object {
    const val AREA_TYPE = "AREA"
  }
}

@Entity
@DiscriminatorValue(SubArea.AREA_TYPE)
class SubArea(
  areaCode: String,
  description: String,
  listSequence: Int,
  parentAreaCode: String,
  active: Boolean,
  expiryDate: LocalDate?,
  type: TypeOfArea?,
) : AbstractArea(
  areaCode = areaCode,
  description = description,
  parentAreaCode = parentAreaCode,
  listSequence = listSequence,
  active = active,
  expiryDate = expiryDate,
  type = type,
) {
  companion object {
    const val AREA_TYPE = "SUB_AREA"
  }
}

@Entity
@DiscriminatorValue(Region.AREA_TYPE)
class Region(
  areaCode: String,
  description: String,
  listSequence: Int,
  active: Boolean,
  expiryDate: LocalDate?,
  type: TypeOfArea?,
) : AbstractArea(
  areaCode = areaCode,
  description = description,
  parentAreaCode = null,
  listSequence = listSequence,
  active = active,
  expiryDate = expiryDate,
  type = type,
) {
  companion object {
    const val AREA_TYPE = "REGION"
  }
}
