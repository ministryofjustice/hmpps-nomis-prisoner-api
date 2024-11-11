package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashScreen

interface SplashScreenRepository : CrudRepository<SplashScreen, Long> {
  fun findByModuleName(moduleName: String): SplashScreen?
}
