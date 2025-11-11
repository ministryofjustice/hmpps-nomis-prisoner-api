package uk.gov.justice.digital.hmpps.nomisprisonerapi.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.atLeastOnce
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.UpsertCSIPRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.UpsertCSIPResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances.CreateVisitBalanceAdjustmentRequest
import java.time.LocalDate

class AuditResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderBookingRepository: OffenderBookingRepository

  @Autowired
  private lateinit var repository: Repository

  @MockitoSpyBean
  lateinit var auditConnectionHandler: AuditConnectionHandler

  @Nested
  inner class NoAuditAnnotation {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234TT", firstName = "Bob", lastName = "Smith") {
          booking(agencyLocationId = "MDI")
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteAllCSIPReports()
      repository.deleteOffenders()
    }

    @Test
    fun `Audit sets default audit module`() {
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

      verify(auditConnectionHandler, atLeastOnce()).applyAuditModule(any())
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

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
      repository.deleteStaff()
    }

    @Test
    fun `Audit sets specific audit module`() {
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

      verify(auditConnectionHandler, atLeastOnce()).applyAuditModule(any())
    }
  }
}
