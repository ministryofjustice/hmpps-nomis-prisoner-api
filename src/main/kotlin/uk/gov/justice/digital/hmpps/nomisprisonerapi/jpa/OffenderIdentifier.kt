package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import java.io.Serializable
import java.time.LocalDate

@Embeddable
class OffenderIdentifierPK(
  @JoinColumn(name = "OFFENDER_ID", nullable = false)
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val offender: Offender,

  @Column(name = "OFFENDER_ID_SEQ", nullable = false)
  val sequence: Long = 0,
) : Serializable

@Entity
@Table(name = "OFFENDER_IDENTIFIERS")
class OffenderIdentifier(
  @EmbeddedId
  val id: OffenderIdentifierPK,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'${IdentifierType.ID_TYPE}'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "IDENTIFIER_TYPE", referencedColumnName = "code", nullable = false)),
    ],
  )
  val identifierType: IdentifierType,

  @Column(name = "IDENTIFIER", nullable = false)
  val identifier: String,

  @Column(name = "ISSUED_AUTHORITY_TEXT")
  val issuedAuthority: String?,

  @Column(name = "ISSUED_DATE")
  val issuedDate: LocalDate?,

  @Column(name = "VERIFIED_FLAG")
  @Convert(converter = YesNoConverter::class)
  var verified: Boolean? = false,

  @Column(name = "CASELOAD_TYPE")
  private val caseloadType: String = "INST",
) : NomisAuditableEntity() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderIdentifier

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
