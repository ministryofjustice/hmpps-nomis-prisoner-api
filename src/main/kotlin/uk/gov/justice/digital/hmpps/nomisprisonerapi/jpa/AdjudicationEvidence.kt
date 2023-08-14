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
import java.time.LocalDate

@Entity
@Table(name = "AGY_INC_INV_STATEMENTS")
class AdjudicationEvidence(
  @SequenceGenerator(
    name = "AGY_II_STATEMENT_ID",
    sequenceName = "AGY_II_STATEMENT_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "AGY_II_STATEMENT_ID")
  @Id
  @Column(name = "AGY_II_STATEMENT_ID")
  val id: Long = 0,

  @Column
  val statementDetail: String,

  @Column(name = "DATE_OF_STATEMENT_TAKEN")
  val statementDate: LocalDate,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AdjudicationEvidenceType.OIC_STMT_TYP + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "STATEMENT_TYPE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  val statementType: AdjudicationEvidenceType,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_INC_INVESTIGATION_ID")
  val investigation: AdjudicationInvestigation,

) {
  @Generated
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  lateinit var createUsername: String

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationEvidence
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
