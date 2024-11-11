package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(SplashBlockAccessCodeType.SPLASH_BLK)
class SplashBlockAccessCodeType(code: String, description: String) : ReferenceCode(SPLASH_BLK, code, description) {
  companion object {
    const val SPLASH_BLK = "SPLASH_BLK"
    fun pk(code: String): Pk = Pk(SPLASH_BLK, code)
  }
}
