package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBelief
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProfileCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProfileCodeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBeliefRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ProfileCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderBeliefDslMarker

@NomisDataDslMarker
interface OffenderBeliefDsl

@Component
class OffenderBeliefBuilderRepository(
  private val offenderBeliefRepository: OffenderBeliefRepository,
  private val jdbcTemplate: JdbcTemplate,
  private val profileCodeRepository: ProfileCodeRepository,
) {
  fun profileCodeOf(code: String): ProfileCode = profileCodeRepository.findByIdOrNull(ProfileCodeId("RELF", code))!!
  fun save(offenderBelief: OffenderBelief): OffenderBelief = offenderBeliefRepository.saveAndFlush(offenderBelief)
  fun updateCreateDatetime(offenderBelief: OffenderBelief, whenCreated: LocalDateTime) {
    jdbcTemplate.update("update OFFENDER_BELIEFS set CREATE_DATETIME = ? where BELIEF_ID = ?", whenCreated, offenderBelief.beliefId)
  }
  fun updateCreateUsername(offenderBelief: OffenderBelief, whoCreated: String) {
    jdbcTemplate.update("update OFFENDER_BELIEFS set CREATE_USER_ID = ? where BELIEF_ID = ?", whoCreated, offenderBelief.beliefId)
  }
}

@Component
class OffenderBeliefBuilderFactory(val repository: OffenderBeliefBuilderRepository) {
  fun builder() = OffenderBeliefBuilder(repository)
}

class OffenderBeliefBuilder(
  private val repository: OffenderBeliefBuilderRepository,
) : OffenderBeliefDsl {

  private lateinit var offenderBelief: OffenderBelief

  fun build(
    booking: OffenderBooking,
    offender: Offender,
    beliefCode: String,
    startDate: LocalDate,
    endDate: LocalDate?,
    changeReason: Boolean?,
    comments: String?,
    verified: Boolean?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
  ): OffenderBelief =
    OffenderBelief(
      booking = booking,
      rootOffender = offender,
      beliefCode = repository.profileCodeOf(beliefCode),
      startDate = startDate,
      endDate = endDate,
      changeReason = changeReason,
      comments = comments,
      verified = verified,
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
}
