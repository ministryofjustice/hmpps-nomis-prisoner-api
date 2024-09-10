package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("CODE")
class ProfileTypeCode(type: String, category: String? = null, description: String) : ProfileType(type, category, description)
