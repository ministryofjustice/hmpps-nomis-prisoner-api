package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
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
          value = "'" + SplashAccessBlockedType.SPLASH_BLK + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "BLOCK_ACCESS_CODE", referencedColumnName = "code", nullable = true)),
    ],
  )
  var accessBlockedType: SplashAccessBlockedType,

  @Column
  val blockedText: String?,

  @OneToMany(mappedBy = "splashScreen", cascade = [CascadeType.ALL], orphanRemoval = true)
  val conditions: MutableList<SplashCondition> = mutableListOf(),
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as SplashScreen
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  companion object {
    // Note that this uses two * at the beginning, as opposed to ServiceAgencySwitch that uses one.
    const val SPLASH_ALL_PRISONS = "**ALL**"
  }
}

fun SplashScreen.blockedList(): List<String> = if (isBlockedAccess()) {
  this.conditions.map { it.value }
} else if (isConditionalAccess()) {
  this.conditions.filter { it.accessBlocked }.map { it.value }
} else {
  listOf()
}

fun SplashScreen.isBlockedAccess(): Boolean = accessBlockedType.code == "YES"
fun SplashScreen.isConditionalAccess(): Boolean = accessBlockedType.code == "COND"
fun SplashScreen.isNotBlockedAccess(): Boolean = accessBlockedType.code == "NO"
