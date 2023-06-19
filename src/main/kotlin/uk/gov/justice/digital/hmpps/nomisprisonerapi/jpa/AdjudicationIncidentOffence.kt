package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula

@Entity
@Table(name = "OIC_OFFENCES")
class AdjudicationIncidentOffence(
  @Id
  @Column(name = "OIC_OFFENCE_ID")
  val id: Long = 0,
  @Column(name = "OIC_OFFENCE_CODE")
  val code: String,
  @Column(name = "DESCRIPTION")
  val description: String,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AdjudicationOffenceType.OIC_OFN_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "OIC_OFFENCE_TYPE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val type: AdjudicationOffenceType?,

) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationIncidentOffence
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
