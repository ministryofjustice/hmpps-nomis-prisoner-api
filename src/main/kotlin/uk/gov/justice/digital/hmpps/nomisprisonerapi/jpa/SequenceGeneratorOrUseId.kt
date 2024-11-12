package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.HibernateException
import org.hibernate.annotations.IdGeneratorType
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.enhanced.SequenceStyleGenerator
import org.hibernate.service.ServiceRegistry
import org.hibernate.type.Type
import java.util.*

@IdGeneratorType(SequenceGeneratorOrUseId::class)
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SequenceOrUseId(
  val name: String,
  val startWith: Int = 1,
  val incrementBy: Int = 1,
)

class SequenceGeneratorOrUseId(private val config: SequenceOrUseId) : SequenceStyleGenerator() {

  override fun configure(type: Type, params: Properties, serviceRegistry: ServiceRegistry) {
    val appliedParams = Properties()
    appliedParams.putAll(params)
    appliedParams[INITIAL_PARAM] = config.startWith
    appliedParams[INCREMENT_PARAM] = config.incrementBy
    appliedParams[SEQUENCE_PARAM] = config.name

    super.configure(type, appliedParams, serviceRegistry)
  }

  @Throws(HibernateException::class)
  override fun generate(session: SharedSessionContractImplementor, entity: Any): Any {
    val id = session.getEntityPersister(null, entity).getIdentifier(entity, session)
    return id.takeIf { id != 0L } ?: super.generate(session, entity)
  }
}
