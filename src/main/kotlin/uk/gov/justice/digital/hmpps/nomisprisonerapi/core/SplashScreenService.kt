package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashCondition
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashScreen
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.blockedList
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SplashScreenRepository

@Service
@Transactional
class SplashScreenService(
  private val splashScreenRepository: SplashScreenRepository,
) {
  fun getSplashScreen(moduleName: String): SplashScreenDto = splashScreenRepository.findByModuleName(moduleName)
    ?.toDto()
    ?: throw NotFoundException("Splash screen with screen/module name $moduleName does not exist")

  fun getBlockedPrisons(moduleName: String): List<PrisonDto> = splashScreenRepository.findByModuleName(moduleName)
    ?.blockedList()?.map { PrisonDto(prisonId = it) }
    ?: listOf()
}

fun SplashScreen.toDto() = SplashScreenDto(
  moduleName = moduleName,
  warningText = warningText,
  accessBlockedType = accessBlockedType.toCodeDescription(),
  blockedText = blockedText,
  conditions = this.conditions.map { it.toDto() },
)
fun SplashCondition.toDto() = SplashConditionDto(
  prisonId = value,
  accessBlocked = accessBlocked,
  type = type.toCodeDescription(),
)
