package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.HibernateException
import org.hibernate.annotations.IdGeneratorType
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.GeneratorCreationContext
import org.hibernate.id.enhanced.SequenceStyleGenerator
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

  override fun configure(creationContext: GeneratorCreationContext?, parameters: Properties) {
    parameters[INITIAL_PARAM] = config.startWith
    parameters[INCREMENT_PARAM] = config.incrementBy
    parameters[SEQUENCE_PARAM] = config.name
    super.configure(creationContext, parameters)
  }

  @Throws(HibernateException::class)
  override fun generate(session: SharedSessionContractImplementor, entity: Any): Any {
    val id = session.getEntityPersister(null, entity).getIdentifier(entity, session)
    return id.takeIf { id != 0L } ?: super.generate(session, entity)
  }

  override fun allowAssignedIdentifiers(): Boolean = true
}
