package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseIdentifierPK

@DslMarker
annotation class OffenderCaseIdentifierDslMarker

@NomisDataDslMarker
interface OffenderCaseIdentifierDsl

@Component
class OffenderCaseIdentifierBuilderFactory {
  fun builder() = OffenderCaseIdentifierBuilder()
}

class OffenderCaseIdentifierBuilder : OffenderCaseIdentifierDsl {

  private lateinit var caseIdentifier: OffenderCaseIdentifier

  fun build(
    courtCase: CourtCase,
    reference: String,
    type: String,
  ): OffenderCaseIdentifier =
    OffenderCaseIdentifier(
      id = OffenderCaseIdentifierPK(identifierType = type, reference = reference, courtCase = courtCase),
    )
      .also { caseIdentifier = it }
}
