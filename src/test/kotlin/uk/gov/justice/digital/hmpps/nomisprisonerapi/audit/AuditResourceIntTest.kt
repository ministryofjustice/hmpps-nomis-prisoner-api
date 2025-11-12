package uk.gov.justice.digital.hmpps.nomisprisonerapi.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.UpsertCSIPRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.UpsertCSIPResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.locations.CreateLocationRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.locations.LocationIdResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances.CreateVisitBalanceAdjustmentRequest
import java.time.LocalDate

class AuditResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @MockitoSpyBean
  lateinit var storedProcedureRepository: StoredProcedureRepository

  @Autowired
  private lateinit var offenderBookingRepository: OffenderBookingRepository

  @Autowired
  private lateinit var repository: Repository

  @AfterEach
  fun tearDown() {
    repository.deleteAllCSIPReports()
    repository.deleteOffenders()
    repository.deleteStaff()
  }

  @Nested
  inner class NoAuditAnnotation {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "FRED", lastName = "JAMES") {
          account(username = "FRED.JAMES")
        }
        offender(nomsId = "A1234TT", firstName = "Bob", lastName = "Smith") {
          booking(agencyLocationId = "MDI")
        }
      }
    }

    @Test
    fun `Audit is not called`() {
      val response = webTestClient.put().uri("/csip")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(createUpsertCSIPRequestMinimalData())
        .exchange()
        .expectStatus().isOk
        .expectBody(UpsertCSIPResponse::class.java)
        .returnResult().responseBody!!

      // Ensure the csip was created
      assertThat(response.nomisCSIPReportId).isNotZero

      verify(storedProcedureRepository, never()).audit(anyString())
      verify(storedProcedureRepository, never()).resetAudit()
    }

    private fun createUpsertCSIPRequestMinimalData(csipReportId: Long? = null) = UpsertCSIPRequest(
      id = csipReportId,
      offenderNo = "A1234TT",
      incidentDate = LocalDate.parse("2023-12-15"),
      typeCode = "VPA",
      locationCode = "EXY",
      areaOfWorkCode = "KIT",
      reportedBy = "Jill Reporter",
      reportedDate = LocalDate.parse("2024-05-12"),
    )
  }

  @Nested
  inner class AuditAnnotationNoParameter {

    @Test
    fun `Audit sync passes`() {
      val requestBody = createLocationRequest()
      val locationAuditId = webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .body(BodyInserters.fromValue(requestBody))
        .exchange()
        .expectStatus().isCreated
        .expectBody(LocationIdResponse::class.java)
        .returnResult().responseBody!!.locationId

      // Ensure the location was created
      assertThat(locationAuditId).isNotZero
      verify(storedProcedureRepository, times(1)).audit("DPS_SYNCHRONISATION")
      verify(storedProcedureRepository, times(1)).resetAudit()
      // Clear up
      repository.deleteAgencyInternalLocationById(locationAuditId)
    }

    @Test
    fun `Setting Audit module fails`() {
      whenever(storedProcedureRepository.audit("DPS_SYNCHRONISATION"))
        .thenThrow(RuntimeException("Test audit exception thrown by spybean"))

      val requestBody = createLocationRequest()
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .body(BodyInserters.fromValue(requestBody))
        .exchange()
        .expectStatus().is5xxServerError

      // Ensure the location was not created, i.e. the insert never happened
      assertThat(repository.lookupAgencyInternalLocationByDescription(requestBody.description)).isNull()
      verify(storedProcedureRepository, times(1)).audit("DPS_SYNCHRONISATION")
      verify(storedProcedureRepository, times(1)).resetAudit()
    }

    @Test
    fun `Audit reset fails`() {
      whenever(storedProcedureRepository.resetAudit()).thenThrow(RuntimeException("Test reset exception thrown by spybean"))

      val requestBody = createLocationRequest()
      webTestClient.post().uri("/locations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .body(BodyInserters.fromValue(requestBody))
        .exchange()
        .expectStatus().is5xxServerError

      // Ensure the location was not created, i.e. the insert was rolled back
      assertThat(repository.lookupAgencyInternalLocationByDescription(requestBody.description)).isNull()
    }

    private val createLocationRequest: () -> CreateLocationRequest = {
      CreateLocationRequest(
        certified = true,
        locationType = "LAND",
        prisonId = "MDI",
        locationCode = "5",
        description = "LEI-A-2-TEST",
        parentLocationId = -1L,
        tracking = false,
      )
    }
  }

  @Nested
  inner class AuditAnnotationWithParameter {
    private var activeBookingId = 0L
    private var staffUserId = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staffUserId = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }.id

        offender(nomsId = "A1234AB") {
          activeBookingId = booking {
            visitBalance {
              visitBalanceAdjustment(authorisedStaffId = staffUserId)
              visitBalanceAdjustment(authorisedStaffId = staffUserId)
            }
          }.bookingId
        }
      }
    }

    @Test
    fun `Audit sync passes`() {
      repository.runInTransaction {
        val booking = offenderBookingRepository.findByIdOrNull(activeBookingId)!!
        assertThat(booking.visitBalanceAdjustments).hasSize(2)
      }
      webTestClient.post().uri("/prisoners/A1234AB/visit-balance-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          CreateVisitBalanceAdjustmentRequest(
            adjustmentDate = LocalDate.parse("2025-03-03"),
            authorisedUsername = "JANESTAFF",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // Ensure the visit balance adjustment was created
      repository.runInTransaction {
        val bookingAfterPost = offenderBookingRepository.findByIdOrNull(activeBookingId)!!
        assertThat(bookingAfterPost.visitBalanceAdjustments).hasSize(3)
      }
      verify(storedProcedureRepository, times(1)).audit("DPS_SYNCHRONISATION_VB")
      verify(storedProcedureRepository, times(1)).resetAudit()
    }

    @Test
    fun `Setting audit module fails`() {
      repository.runInTransaction {
        val booking = offenderBookingRepository.findByIdOrNull(activeBookingId)!!
        assertThat(booking.visitBalanceAdjustments).hasSize(2)
      }

      whenever(storedProcedureRepository.audit("DPS_SYNCHRONISATION_VB"))
        .thenThrow(RuntimeException("Test audit exception thrown by spybean"))

      webTestClient.post().uri("/prisoners/A1234AB/visit-balance-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          CreateVisitBalanceAdjustmentRequest(
            adjustmentDate = LocalDate.parse("2025-03-03"),
            authorisedUsername = "JANESTAFF",
          ),
        )
        .exchange()
        .expectStatus().is5xxServerError

      // Ensure the visit balance adjustment was not created, i.e. the insert never happened
      repository.runInTransaction {
        val bookingAfterPost = offenderBookingRepository.findByIdOrNull(activeBookingId)!!
        assertThat(bookingAfterPost.visitBalanceAdjustments).hasSize(2)
      }
      verify(storedProcedureRepository, times(1)).audit("DPS_SYNCHRONISATION_VB")
      verify(storedProcedureRepository, times(1)).resetAudit()
    }

    @Test
    fun `Audit reset fails`() {
      whenever(storedProcedureRepository.resetAudit())
        .thenThrow(RuntimeException("Test reset exception thrown by spybean"))

      webTestClient.post().uri("/prisoners/A1234AB/visit-balance-adjustments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          CreateVisitBalanceAdjustmentRequest(
            adjustmentDate = LocalDate.parse("2025-03-03"),
            authorisedUsername = "JANESTAFF",
          ),
        )
        .exchange()
        .expectStatus().is5xxServerError

      // Ensure the visit balance adjustment was not created, i.e. the insert never happened
      repository.runInTransaction {
        val bookingAfterPost = offenderBookingRepository.findByIdOrNull(activeBookingId)!!
        assertThat(bookingAfterPost.visitBalanceAdjustments).hasSize(2)
      }
      verify(storedProcedureRepository, times(1)).audit("DPS_SYNCHRONISATION_VB")
    }
  }
}
