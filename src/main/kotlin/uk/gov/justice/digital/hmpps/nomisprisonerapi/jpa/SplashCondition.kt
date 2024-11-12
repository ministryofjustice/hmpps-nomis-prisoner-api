package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter

@Entity
@Table(name = "SPLASH_CONDITIONS")
data class SplashCondition(
  @Id
  @Column(name = "SPLASH_CONDITION_ID")
  @SequenceGenerator(name = "SPLASH_CONDITION_ID", sequenceName = "SPLASH_CONDITION_ID", allocationSize = 1)
  @GeneratedValue(generator = "SPLASH_CONDITION_ID")
  var id: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "SPLASH_ID")
  val splashScreen: SplashScreen,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + SplashConditionType.SPLASH_COND + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "CONDITION_TYPE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val type: SplashConditionType,

  @Column(name = "CONDITION_VALUE")
  val value: String,

  @Column(name = "BLOCK_ACCESS_YORN")
  @Convert(converter = YesNoConverter::class)
  val accessBlocked: Boolean = false,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as SplashCondition
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
