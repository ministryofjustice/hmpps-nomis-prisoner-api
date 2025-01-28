package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPTemplate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IWPTemplateRepository

@DslMarker
annotation class IWPTemplateDslMarker

@NomisDataDslMarker
interface IWPTemplateDsl

@Component
class IWPTemplateBuilderFactory(
  private val repository: IWPTemplateBuilderRepository,
) {
  fun builder() = IWPTemplateBuilder(repository)
}

@Component
class IWPTemplateBuilderRepository(
  private val repository: IWPTemplateRepository,
) {
  fun save(template: IWPTemplate): IWPTemplate = repository.save(template)
}

class IWPTemplateBuilder(
  private val repository: IWPTemplateBuilderRepository,
) : IWPTemplateDsl {

  fun build(
    name: String,
    description: String?,
  ): IWPTemplate = IWPTemplate(
    name = name,
    description = description,
  )
    .let { repository.save(it) }
}
