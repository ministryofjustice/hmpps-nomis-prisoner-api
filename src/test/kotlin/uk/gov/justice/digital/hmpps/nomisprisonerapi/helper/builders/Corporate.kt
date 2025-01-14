package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Caseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class CorporateDslMarker

@NomisDataDslMarker
interface CorporateDsl {
  @CorporateTypeDslMarker
  fun type(
    typeCode: String = "YOTWORKER",
    dsl: CorporateTypeDsl.() -> Unit = { },
  ): CorporateType
}

@Component
class CorporateBuilderFactory(
  private val repository: CorporateBuilderRepository,
  private val corporateTypeBuilderFactory: CorporateTypeBuilderFactory,
) {
  fun builder(): CorporateBuilder = CorporateBuilder(repository, corporateTypeBuilderFactory)
}

@Component
class CorporateBuilderRepository(
  private val corporateRepository: CorporateRepository,
  private val caseloadRepository: CaseloadRepository,
  private val jdbcTemplate: JdbcTemplate,
) {
  fun save(corporate: Corporate): Corporate = corporateRepository.saveAndFlush(corporate)
  fun caseloadOf(code: String?): Caseload? = code?.let { caseloadRepository.findByIdOrNull(it) }
  fun updateCreateDatetime(corporate: Corporate, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update CORPORATES set CREATE_DATETIME = ? where CORPORATE_ID = ?", whenCreated, corporate.id)
  }
  fun updateCreateUsername(corporate: Corporate, whoCreated: String) {
    jdbcTemplate.update("update CORPORATES set CREATE_USER_ID = ? where CORPORATE_ID = ?", whoCreated, corporate.id)
  }
}

class CorporateBuilder(
  private val repository: CorporateBuilderRepository,
  private val corporateTypeBuilderFactory: CorporateTypeBuilderFactory,
) : CorporateDsl {
  private lateinit var corporate: Corporate

  fun build(
    corporateName: String,
    caseloadId: String?,
    commentText: String?,
    suspended: Boolean,
    feiNumber: String?,
    active: Boolean,
    expiryDate: LocalDate?,
    taxNo: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): Corporate = Corporate(
    corporateName = corporateName,
    caseload = caseloadId?.let { repository.caseloadOf(it) },
    commentText = commentText,
    suspended = suspended,
    feiNumber = feiNumber,
    active = active,
    expiryDate = expiryDate,
    taxNo = taxNo,
  )
    .let { repository.save(it) }
    .also {
      if (whenCreated != null) {
        repository.updateCreateDatetime(it, whenCreated)
      }
      if (whoCreated != null) {
        repository.updateCreateUsername(it, whoCreated)
      }
    }
    .also { corporate = it }

  override fun type(typeCode: String, dsl: CorporateTypeDsl.() -> Unit): CorporateType = corporateTypeBuilderFactory.builder().let { builder ->
    builder.build(
      corporate = corporate,
      typeCode = typeCode,
    )
      .also { corporate.types += it }
      .also { builder.apply(dsl) }
  }
}
