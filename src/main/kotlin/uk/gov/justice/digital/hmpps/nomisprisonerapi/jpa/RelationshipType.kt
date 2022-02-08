package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(RelationshipType.RELATIONSHIP)
class RelationshipType(code: String, description: String) : ReferenceCode(RELATIONSHIP, code, description) {

  companion object {
    const val RELATIONSHIP = "RELATIONSHIP"
    fun pk(code: String): Pk = Pk(RELATIONSHIP, code)
  }
}
