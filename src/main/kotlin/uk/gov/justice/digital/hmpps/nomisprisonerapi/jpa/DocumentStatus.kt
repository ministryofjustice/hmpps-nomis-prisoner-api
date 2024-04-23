package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(DocumentStatus.DOCUMENT_STS)
class DocumentStatus(code: String, description: String) : ReferenceCode(DOCUMENT_STS, code, description) {

  companion object {
    const val DOCUMENT_STS = "DOCUMENT_STS"
    fun pk(code: String): Pk = Pk(DOCUMENT_STS, code)
  }
}
