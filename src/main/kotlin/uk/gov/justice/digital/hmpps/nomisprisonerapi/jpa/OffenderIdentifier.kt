package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import java.io.Serializable
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "OFFENDER_IDENTIFIERS")
class OffenderIdentifier {
  @Embeddable
  class OffenderIdentifierPK : Serializable {
    @Column(name = "OFFENDER_ID", nullable = false)
    private val offenderId: Long = 0

    @Column(name = "OFFENDER_ID_SEQ", nullable = false)
    private val offenderIdSeq: Long = 0
  }

  @EmbeddedId
  private val offenderIdentifierPK: OffenderIdentifierPK? = null

  @ManyToOne
  @JoinColumn(name = "OFFENDER_ID", insertable = false, updatable = false)
  private val offender: Offender? = null

  @Column(name = "IDENTIFIER_TYPE", nullable = false)
  private val identifierType: String? = null

  @Column(name = "IDENTIFIER", nullable = false)
  private val identifier: String? = null

  @Column(name = "ISSUED_DATE")
  private val issuedDate: LocalDate? = null

  @Column(name = "ROOT_OFFENDER_ID")
  private val rootOffenderId: Long? = null

  @Column(name = "CASELOAD_TYPE")
  private val caseloadType: String? = null
  fun isPnc(): Boolean {
    return "PNC".equals(identifierType, ignoreCase = true)
  }

  fun isCro(): Boolean {
    return "CRO".equals(identifierType, ignoreCase = true)
  }
}
