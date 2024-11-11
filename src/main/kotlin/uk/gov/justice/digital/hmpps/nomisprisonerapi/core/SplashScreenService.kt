package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashScreen
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SplashScreenRepository

@Service
@Transactional
class SplashScreenService(
  private val splashScreenRepository: SplashScreenRepository,
) {
  fun getSplashScreens(moduleName: String): SplashScreenDto =
    splashScreenRepository.findByModuleName(moduleName)
      ?.toDto()
      ?: throw NotFoundException("Splash screen with screen/module name $moduleName does not exist")
}

fun SplashScreen.toDto() = SplashScreenDto(
  moduleName = moduleName,
  warningText = warningText,
  blockAccessCode = blockAccessCode.toCodeDescription(),
  blockedText = blockedText,
)
