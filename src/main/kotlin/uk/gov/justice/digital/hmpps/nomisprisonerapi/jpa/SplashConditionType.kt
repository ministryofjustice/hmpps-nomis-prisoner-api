package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(SplashConditionType.SPLASH_COND)
class SplashConditionType(code: String, description: String) : ReferenceCode(SPLASH_COND, code, description) {
  companion object {
    const val SPLASH_COND = "SPLASH_COND"
    fun pk(code: String): Pk = Pk(SPLASH_COND, code)
  }
}
