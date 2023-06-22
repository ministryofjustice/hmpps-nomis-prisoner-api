package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class DataDslMarker

@DataDslMarker
interface DataDsl {
  fun staff(firstName: String, lastName: String, dsl: StaffDsl.() -> Unit = {}): Staff

  fun adjudicationIncident(
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

  fun offender(
    nomsId: String = "A5194DY",
    lastName: String = "NTHANDA",
    firstName: String = "LEKAN",
    birthDate: LocalDate = LocalDate.of(1965, 7, 19),
    genderCode: String = "M",
    dsl: OffenderDsl.() -> Unit = {},
  ): Offender
}

@DataDslMarker
interface StaffDsl

@DataDslMarker
interface OffenderDsl {
  fun booking(
    bookingBeginDate: LocalDateTime = LocalDateTime.now(),
    active: Boolean = true,
    inOutStatus: String = "IN",
    youthAdultCode: String = "N",
    visitBalanceBuilder: VisitBalanceBuilder? = null,
    agencyLocationId: String = "BXI",
    dsl: BookingDsl.() -> Unit = {},
  )
}

@DataDslMarker
interface BookingDsl {
  fun adjudicationParty(
    incident: AdjudicationIncident,
    adjudicationNumber: Long = 1224,
    comment: String = "party comment",
    partyAddedDate: LocalDate = LocalDate.of(2023, 5, 10),
    dsl: AdjudicationPartyDsl.() -> Unit = {},
  )
}

@DataDslMarker
interface AdjudicationIncidentDsl {
  fun repair(
    repairType: String = "CLEA",
    comment: String? = null,
    repairCost: BigDecimal? = null,
    dsl: AdjudicationRepairDsl.() -> Unit = {},
  )
}

@DataDslMarker
interface AdjudicationPartyDsl {
  fun investigation(
    investigator: Staff,
    comment: String? = null,
    assignedDate: LocalDate = LocalDate.now(),
    dsl: AdjudicationInvestigationDsl.() -> Unit = {},
  )

  fun charge(
    offenceCode: String = "51:1B",
    guiltyEvidence: String? = null,
    reportDetail: String? = null,
    dsl: AdjudicationChargeDsl.() -> Unit = {},
  )
}

@DataDslMarker
interface AdjudicationRepairDsl

@DataDslMarker
interface AdjudicationInvestigationDsl {
  fun evidence(
    detail: String = "Knife found",
    type: String = "WEAP",
    date: LocalDate = LocalDate.now(),
    dsl: AdjudicationEvidenceDsl.() -> Unit = {},
  )
}

@DataDslMarker
interface AdjudicationEvidenceDsl

@DataDslMarker
interface AdjudicationChargeDsl
