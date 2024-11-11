package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
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

@Entity
@Table(name = "SPLASH_SCREENS")
data class SplashScreen(
  @Id
  @Column(name = "SPLASH_ID", nullable = false)
  @SequenceGenerator(name = "SPLASH_ID", sequenceName = "SPLASH_ID", allocationSize = 1)
  @GeneratedValue(generator = "SPLASH_ID")
  var id: Long = 0,

  @Column(nullable = false)
  val moduleName: String,

  // ALL are null - so left unmapped
  // @Column
  // val functionName: String?,

  @Column
  val warningText: String?,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + SplashBlockAccessCodeType.SPLASH_BLK + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "BLOCK_ACCESS_CODE", referencedColumnName = "code", nullable = true)),
    ],
  )
  var blockAccessCode: SplashBlockAccessCodeType,

  @Column
  val blockedText: String?,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as SplashScreen
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
