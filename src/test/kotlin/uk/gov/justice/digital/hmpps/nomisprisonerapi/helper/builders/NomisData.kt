package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalDateTime

@Component
@Transactional
class NomisDataBuilder(
  private val programServiceBuilderFactory: ProgramServiceBuilderFactory? = ProgramServiceBuilderFactory(),
  private val offenderBuilderFactory: OffenderBuilderFactory? = null, // note this means the offender DSL is not available in unit tests whereas programService is.
  private val staffBuilderFactory: StaffBuilderFactory? = null,
  private val adjudicationIncidentBuilderFactory: AdjudicationIncidentBuilderFactory? = null,
) {
  fun build(dsl: NomisData.() -> Unit) = NomisData(programServiceBuilderFactory, offenderBuilderFactory, staffBuilderFactory, adjudicationIncidentBuilderFactory).apply(dsl)
}

class NomisData(
  private val programServiceBuilderFactory: ProgramServiceBuilderFactory? = null,
  private val offenderBuilderFactory: OffenderBuilderFactory? = null,
  private val staffBuilderFactory: StaffBuilderFactory? = null,
  private val adjudicationIncidentBuilderFactory: AdjudicationIncidentBuilderFactory? = null,
) : NomisDataDsl {
  @StaffDslMarker
  override fun staff(firstName: String, lastName: String, dsl: StaffDsl.() -> Unit): Staff =
    staffBuilderFactory!!.builder()
      .let { builder ->
        builder.build(lastName, firstName)
          .also {
            builder.apply(dsl)
          }
      }

  @AdjudicationIncidentDslMarker
  override fun adjudicationIncident(
    whenCreated: LocalDateTime,
    incidentDetails: String,
    reportedDateTime: LocalDateTime,
    reportedDate: LocalDate,
    incidentDateTime: LocalDateTime,
    incidentDate: LocalDate,
    prisonId: String,
    agencyInternalLocationId: Long,
    reportingStaff: Staff,
    dsl: AdjudicationIncidentDsl.() -> Unit,
  ): AdjudicationIncident = adjudicationIncidentBuilderFactory!!.builder()
    .let { builder ->
      builder.build(
        whenCreated = whenCreated,
        incidentDetails = incidentDetails,
        reportedDateTime = reportedDateTime,
        reportedDate = reportedDate,
        incidentDateTime = incidentDateTime,
        incidentDate = incidentDate,
        prisonId = prisonId,
        agencyInternalLocationId = agencyInternalLocationId,
        reportingStaff = reportingStaff,
      )
        .also {
          builder.apply(dsl)
        }
    }

  @OffenderDslMarker
  override fun offender(
    nomsId: String,
    lastName: String,
    firstName: String,
    birthDate: LocalDate,
    genderCode: String,
    dsl: OffenderDsl.() -> Unit,
  ): Offender =
    offenderBuilderFactory!!.builder()
      .let { builder ->
        builder.build(nomsId, lastName, firstName, birthDate, genderCode)
          .also {
            builder.apply(dsl)
          }
      }

  @ProgramServiceDslMarker
  override fun programService(
    programCode: String,
    programId: Long,
    description: String,
    active: Boolean,
    dsl: ProgramServiceDsl.() -> Unit,
  ): ProgramService =
    programServiceBuilderFactory!!.builder()
      .let { builder ->
        builder.build(programCode, programId, description, active)
          .also {
            builder.apply(dsl)
          }
      }
}

@NomisDataDslMarker
interface NomisDataDsl {
  @OffenderDslMarker
  fun offender(
    nomsId: String = "A5194DY",
    lastName: String = "NTHANDA",
    firstName: String = "LEKAN",
    birthDate: LocalDate = LocalDate.of(1965, 7, 19),
    genderCode: String = "M",
    dsl: OffenderDsl.() -> Unit = {},
  ): Offender

  @ProgramServiceDslMarker
  fun programService(
    programCode: String = "INTTEST",
    programId: Long = 20,
    description: String = "test program",
    active: Boolean = true,
    dsl: ProgramServiceDsl.() -> Unit = {},
  ): ProgramService

  @StaffDslMarker
  fun staff(firstName: String = "AAYAN", lastName: String = "AHMAD", dsl: StaffDsl.() -> Unit = {}): Staff

  @AdjudicationIncidentDslMarker
  fun adjudicationIncident(
    whenCreated: LocalDateTime = LocalDateTime.now(),
    incidentDetails: String = "Big fight",
    reportedDateTime: LocalDateTime = LocalDateTime.now(),
    reportedDate: LocalDate = LocalDate.now(),
    incidentDateTime: LocalDateTime = LocalDateTime.now(),
    incidentDate: LocalDate = LocalDate.now(),
    prisonId: String = "MDI",
    agencyInternalLocationId: Long = -41,
    reportingStaff: Staff,
    dsl: AdjudicationIncidentDsl.() -> Unit = {},
  ): AdjudicationIncident
}

@DslMarker
annotation class NomisDataDslMarker
