package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.Id
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

@Entity
@Table(name = "AREAS")
@EntityOpen
class Area(
  @Id
  @Column(name = "AREA_CODE")
  var areaCode: String,

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
        ),
      ),
    ],
  )
  var areaClass: AreaClass,

  @Column(name = "DESCRIPTION")
  var description: String,

  // could map as an Area
  @Column(name = "PARENT_AREA_CODE")
  var parentAreaCode: String? = null,

  @Column(name = "LIST_SEQ")
  var listSequence: Int,

  @Convert(converter = YesNoConverter::class)
  @Column(name = "ACTIVE_FLAG", nullable = false)
  var active: Boolean = true,

  @Column(name = "EXPIRY_DATE")
  var expiryDate: LocalDate? = null,

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
  val type: TypeOfArea? = null,

) : NomisAuditableEntityBasic() {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Area

    return areaCode == other.areaCode
  }

  override fun hashCode(): Int = areaCode.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $areaCode )"
}
