package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_CSIP_FACTORS")
@EntityOpen
data class CSIPFactor(
  @Id
  @Column(name = "CSIP_FACTOR_ID")
  @SequenceGenerator(name = "CSIP_FACTOR_ID", sequenceName = "CSIP_FACTOR_ID", allocationSize = 1)
  @GeneratedValue(generator = "CSIP_FACTOR_ID")
  val id: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "CSIP_ID")
  val csipReport: CSIPReport,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CSIPFactorType.CSIP_FAC + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "CSIP_FACTOR", referencedColumnName = "code", nullable = true)),
    ],
  )
  val type: CSIPFactorType,

  @Column(name = "COMMENTS")
  val comment: String?,
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CSIPFactor

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id)"
  }
}
