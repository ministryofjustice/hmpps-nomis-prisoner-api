package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashBlockAccessCodeType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashScreen
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SplashScreenRepository

@DslMarker
annotation class SplashScreenDslMarker

@NomisDataDslMarker
interface SplashScreenDsl

@Component
class SplashScreenBuilderFactory(
  private val repository: SplashScreenBuilderRepository,
) {
  fun builder() = SplashScreenBuilder(repository)
}

@Component
class SplashScreenBuilderRepository(
  private val splashScreenRepository: SplashScreenRepository,
  val blockAccessCodeRepository: ReferenceCodeRepository<SplashBlockAccessCodeType>,
) {
  fun lookupBlockStatusCode(code: String) = blockAccessCodeRepository.findByIdOrNull(SplashBlockAccessCodeType.pk(code))!!

  fun save(splashScreen: SplashScreen): SplashScreen =
    splashScreenRepository.findByIdOrNull(splashScreen.id)
      ?: splashScreenRepository.save(splashScreen)
}

class SplashScreenBuilder(
  private val repository: SplashScreenBuilderRepository,
) : SplashScreenDsl {
  private lateinit var splashScreen: SplashScreen

  fun build(
    moduleName: String,
    warningText: String?,
    blockAccessCode: String,
    blockedText: String?,
  ): SplashScreen = SplashScreen(
    moduleName = moduleName,
    warningText = warningText,
    blockAccessCode = repository.lookupBlockStatusCode(blockAccessCode),
    blockedText = blockedText,
  )
    .let { repository.save(it) }
    .also { splashScreen = it }
}
