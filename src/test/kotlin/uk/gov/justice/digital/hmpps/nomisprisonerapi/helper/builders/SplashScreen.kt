package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashAccessBlockedType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashCondition
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashScreen
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.SplashScreenRepository

@DslMarker
annotation class SplashScreenDslMarker

@NomisDataDslMarker
interface SplashScreenDsl {
  @SplashConditionDslMarker
  fun splashCondition(
    prisonId: String,
    accessBlocked: Boolean,
    type: String = "CASELOAD",
    dsl: SplashConditionDsl.() -> Unit = {},
  ): SplashCondition
}

@Component
class SplashScreenBuilderFactory(
  private val repository: SplashScreenBuilderRepository,
  private val splashConditionBuilderFactory: SplashConditionBuilderFactory,
) {
  fun builder() = SplashScreenBuilder(repository, splashConditionBuilderFactory)
}

@Component
class SplashScreenBuilderRepository(
  private val splashScreenRepository: SplashScreenRepository,
  val accessBlockedRepository: ReferenceCodeRepository<SplashAccessBlockedType>,
) {
  fun lookupBlockStatusCode(code: String) = accessBlockedRepository.findByIdOrNull(SplashAccessBlockedType.pk(code))!!

  fun save(splashScreen: SplashScreen): SplashScreen =
    splashScreenRepository.findByIdOrNull(splashScreen.id)
      ?: splashScreenRepository.save(splashScreen)
}

class SplashScreenBuilder(
  private val repository: SplashScreenBuilderRepository,
  private val splashConditionBuilderFactory: SplashConditionBuilderFactory,

) : SplashScreenDsl {
  private lateinit var splashScreen: SplashScreen

  fun build(
    moduleName: String,
    warningText: String?,
    accessBlockedCode: String,
    blockedText: String?,
  ): SplashScreen = SplashScreen(
    moduleName = moduleName,
    warningText = warningText,
    accessBlockedType = repository.lookupBlockStatusCode(accessBlockedCode),
    blockedText = blockedText,
  )
    .let { repository.save(it) }
    .also { splashScreen = it }

  override fun splashCondition(
    prisonId: String,
    accessBlocked: Boolean,
    type: String,
    dsl: SplashConditionDsl.() -> Unit,
  ): SplashCondition = splashConditionBuilderFactory.builder()
    .let { builder ->
      builder.build(
        splashScreen = splashScreen,
        prisonId = prisonId,
        accessBlocked = accessBlocked,
        type = type,
      )
        .also { splashScreen.conditions += it }
        .also { builder.apply(dsl) }
    }
}
