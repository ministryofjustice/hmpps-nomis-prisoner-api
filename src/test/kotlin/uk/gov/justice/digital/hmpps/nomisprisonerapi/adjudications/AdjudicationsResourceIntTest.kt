package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AdjudicationIncidentBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AdjudicationPartyBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.StaffBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

class AdjudicationsResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

  lateinit var prisoner: Offender
  lateinit var incident: AdjudicationIncident
  lateinit var staff: Staff

  @DisplayName("Get Adjudication")
  @Nested
  inner class GetAdjudication {
    private var offenderBookingId: Long = 0

    @BeforeEach
    internal fun createPrisonerWithAdjudication() {
      staff = repository.save(StaffBuilder())
      incident = repository.save(AdjudicationIncidentBuilder(reportingStaff = staff))
      prisoner = repository.save(
        OffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder()
              .withAdjudication(incident, AdjudicationPartyBuilder(adjudicationNumber = 9000123)),
          ),
      )

      offenderBookingId = prisoner.latestBooking().bookingId
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(incident)
      repository.delete(prisoner)
      repository.delete(staff)
    }

    @Test
    fun `get adjudication success`() {
    }
  }
}
