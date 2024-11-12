package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashCondition
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashConditionType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashScreen
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class SplashConditionDslMarker

@NomisDataDslMarker
interface SplashConditionDsl

@Component
class SplashConditionBuilderFactory(
  private val repository: SplashConditionBuilderRepository,
) {
  fun builder() = SplashConditionBuilder(repository)
}

@Component
class SplashConditionBuilderRepository(
  val conditionTypeRepository: ReferenceCodeRepository<SplashConditionType>,
) {
  fun lookupConditionType(code: String) = conditionTypeRepository.findByIdOrNull(SplashConditionType.pk(code))!!
}

class SplashConditionBuilder(
  private val repository: SplashConditionBuilderRepository,

) :
  SplashConditionDsl {
  private lateinit var splashCondition: SplashCondition

  fun build(
    splashScreen: SplashScreen,
    prisonId: String,
    type: String,
    accessBlocked: Boolean,
  ): SplashCondition =
    SplashCondition(
      splashScreen = splashScreen,
      type = repository.lookupConditionType(type),
      value = prisonId,
      accessBlocked = accessBlocked,
    )
      .also { splashCondition = it }
}
