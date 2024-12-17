package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
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
import java.io.Serializable

@Embeddable
class PersonIdentifierPK(
  @JoinColumn(name = "PERSON_ID", nullable = false)
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val person: Person,

  @Column(name = "ID_SEQ", nullable = false)
  val sequence: Long = 0,
) : Serializable

@Entity
@Table(name = "PERSON_IDENTIFIERS")
class PersonIdentifier(

  @EmbeddedId
  val id: PersonIdentifierPK,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + IdentifierType.ID_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "IDENTIFIER_TYPE", referencedColumnName = "code", nullable = false)),
    ],
  )
  var identifierType: IdentifierType,

  @Column(name = "IDENTIFIER", nullable = false)
  var identifier: String,

  @Column(name = "ISSUED_AUTHORITY_TEXT")
  var issuedAuthority: String?,

  /*
  Not mapped:
  ISSUED_DATE - always null
   */
) : NomisAuditableEntity() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as PersonIdentifier

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
