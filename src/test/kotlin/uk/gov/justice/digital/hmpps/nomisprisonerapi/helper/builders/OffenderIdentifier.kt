package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IdentifierType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifier
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifierPK
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderIdentifierRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderIdentifierDslMarker

@NomisDataDslMarker
interface OffenderIdentifierDsl

@Component
class OffenderIdentifierBuilderRepository(
  private val offenderIdentifierRepository: OffenderIdentifierRepository,
  private val jdbcTemplate: JdbcTemplate,
  private val identifierTypeRepository: ReferenceCodeRepository<IdentifierType>,
) {
  fun identifierTypeOf(code: String): IdentifierType = identifierTypeRepository.findByIdOrNull(IdentifierType.pk(code))!!
  fun save(offenderIdentifier: OffenderIdentifier): OffenderIdentifier = offenderIdentifierRepository.saveAndFlush(offenderIdentifier)
  fun updateCreateDatetime(offenderIdentifier: OffenderIdentifier, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update OFFENDER_IDENTIFIERS set CREATE_DATETIME = ? where OFFENDER_ID = ? and  ID_SEQ = ?", whenCreated, offenderIdentifier.id.offender.id, offenderIdentifier.id.sequence)
  }
  fun updateCreateUsername(offenderIdentifier: OffenderIdentifier, whoCreated: String) {
    jdbcTemplate.update("update OFFENDER_IDENTIFIERS set CREATE_USER_ID = ? where OFFENDER_ID = ? and  ID_SEQ = ?", whoCreated, offenderIdentifier.id.offender.id, offenderIdentifier.id.sequence)
  }
}

@Component
class OffenderIdentifierBuilderFactory(val repository: OffenderIdentifierBuilderRepository) {
  fun builder() = OffenderIdentifierBuilder(repository)
}

class OffenderIdentifierBuilder(
  private val repository: OffenderIdentifierBuilderRepository,
) : OffenderIdentifierDsl {

  private lateinit var offenderIdentifier: OffenderIdentifier

  fun build(
    offender: Offender,
    sequence: Long,
    type: String,
    identifier: String,
    issuedAuthority: String?,
    issuedDate: LocalDate?,
    verified: Boolean?,
  ): OffenderIdentifier = OffenderIdentifier(
    id = OffenderIdentifierPK(offender, sequence),
    identifierType = repository.identifierTypeOf(type),
    identifier = identifier,
    issuedAuthority = issuedAuthority,
    issuedDate = issuedDate,
    verified = verified,
  )
    .let { repository.save(it) }
    .also { offenderIdentifier = it }
}
