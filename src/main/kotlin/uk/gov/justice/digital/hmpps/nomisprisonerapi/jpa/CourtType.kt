package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue(CourtType.JURISDICTION)
class CourtType : ReferenceCode {
  constructor(code: String, description: String) : super(JURISDICTION, code, description)

  companion object {
    const val JURISDICTION = "JURISDICTION"
  }
}
