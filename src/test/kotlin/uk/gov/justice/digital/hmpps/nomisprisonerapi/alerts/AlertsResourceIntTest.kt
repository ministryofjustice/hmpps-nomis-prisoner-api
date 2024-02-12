package uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.ACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.INACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowStatus.COMP
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowStatus.DONE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class AlertsResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderBookingRepository: OffenderBookingRepository

  @Autowired
  private lateinit var repository: Repository

  @DisplayName("JPA Mapping")
  @Nested
  inner class JpaMapping {
    var bookingId = 0L
    private lateinit var prisoner: Offender

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        prisoner = offender(nomsId = "A1234AB") {
          bookingId = booking {
            alert { }
            alert(
              sequence = 2,
              alertCode = "SC",
              typeCode = "S",
              date = LocalDate.parse("2020-07-19"),
              expiryDate = LocalDate.parse("2025-07-19"),
              authorizePersonText = "Security Team",
              verifiedFlag = true,
              status = INACTIVE,
              commentText = "At risk",
            ) {
              workFlowLog(workActionCode = WorkFlowAction.MODIFIED, workFlowStatus = DONE)
              workFlowLog(workActionCode = WorkFlowAction.VERIFICATION, workFlowStatus = COMP)
            }
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.delete(prisoner)
    }

    @Test
    fun `can insert and read alerts and associated workflow logs`() {
      repository.runInTransaction {
        val booking = offenderBookingRepository.findByIdOrNull(bookingId)

        assertThat(booking!!.alerts).hasSize(2)
        with(booking.alerts.firstOrNull { it.id.sequence == 1L }!!) {
          assertThat(alertDate).isEqualTo(LocalDate.now())
          assertThat(alertStatus).isEqualTo(ACTIVE)
          assertThat(alertCode.code).isEqualTo("XA")
          assertThat(alertCode.description).isEqualTo("Arsonist")
          assertThat(alertType.code).isEqualTo("X")
          assertThat(alertType.description).isEqualTo("Security")
          assertThat(authorizePersonText).isNull()
          assertThat(caseloadType).isEqualTo("INST")
          assertThat(commentText).isNull()
          assertThat(expiryDate).isNull()
          assertThat(verifiedFlag).isFalse()
          assertThat(createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(createUsername).isEqualTo("SA")

          assertThat(workFlows).hasSize(1)
          assertThat(workFlows[0].logs).hasSize(1)
          val log = workFlows[0].logs.first()
          assertThat(log.createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(log.createDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(log.workActionCode.code).isEqualTo("ENT")
          assertThat(log.workActionCode.description).isEqualTo("Data Entry")
          assertThat(log.workFlowStatus).isEqualTo(DONE)
          assertThat(log.locateAgyLoc).isNull()
          assertThat(log.workActionDate).isNull()
          assertThat(log.createUsername).isEqualTo("SA")
        }
        with(booking.alerts.firstOrNull { it.id.sequence == 2L }!!) {
          assertThat(alertDate).isEqualTo(LocalDate.parse("2020-07-19"))
          assertThat(alertStatus).isEqualTo(INACTIVE)
          assertThat(alertCode.code).isEqualTo("SC")
          assertThat(alertCode.description).isEqualTo("Risk to Children")
          assertThat(alertType.code).isEqualTo("S")
          assertThat(alertType.description).isEqualTo("Sexual Offence")
          assertThat(authorizePersonText).isEqualTo("Security Team")
          assertThat(caseloadType).isEqualTo("INST")
          assertThat(commentText).isEqualTo("At risk")
          assertThat(expiryDate).isEqualTo(LocalDate.parse("2025-07-19"))
          assertThat(verifiedFlag).isTrue()
          assertThat(createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(createUsername).isEqualTo("SA")

          assertThat(workFlows).hasSize(1)
          assertThat(workFlows[0].logs).hasSize(3)

          assertThat(workFlows[0].logs[0].workActionCode.code).isEqualTo("ENT")
          assertThat(workFlows[0].logs[0].workFlowStatus).isEqualTo(DONE)
          assertThat(workFlows[0].logs[1].workActionCode.code).isEqualTo("MOD")
          assertThat(workFlows[0].logs[1].workFlowStatus).isEqualTo(DONE)
          assertThat(workFlows[0].logs[2].workActionCode.code).isEqualTo("VER")
          assertThat(workFlows[0].logs[2].workFlowStatus).isEqualTo(COMP)
        }
      }
    }
  }

  @DisplayName("GET /prisoner/booking-id/{bookingId}/alerts/{alertSequence}")
  @Nested
  inner class GetAlert {
    var bookingId = 0L
    private val activeAlertSequence = 1L
    private val inactiveAlertSequence = 2L
    private val alertSequenceWithAuditMinimal = 3L
    private val alertSequenceWithAudit = 4L
    private lateinit var prisoner: Offender

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        prisoner = offender(nomsId = "A1234AB") {
          bookingId = booking {
            alert(
              sequence = activeAlertSequence,
              alertCode = "HPI",
              typeCode = "X",
              date = LocalDate.parse("2023-07-19"),
              expiryDate = null,
              authorizePersonText = null,
              verifiedFlag = false,
              status = ACTIVE,
              commentText = null,
            )
            alert(
              sequence = inactiveAlertSequence,
              alertCode = "SC",
              typeCode = "S",
              date = LocalDate.parse("2020-07-19"),
              expiryDate = LocalDate.parse("2025-07-19"),
              authorizePersonText = "Security Team",
              verifiedFlag = true,
              status = INACTIVE,
              commentText = "At risk",
            )
            alert(
              sequence = alertSequenceWithAuditMinimal,
              alertCode = "HPI",
              typeCode = "X",
            ) {
              audit()
            }
            alert(
              sequence = alertSequenceWithAudit,
              alertCode = "HPI",
              typeCode = "X",
            ) {
              audit(
                createUsername = "JANE.NARK",
                createDatetime = LocalDateTime.parse("2020-01-23T10:23"),
                modifyUserId = "TREV.NACK",
                modifyDatetime = LocalDateTime.parse("2022-02-23T10:23:12"),
                auditTimestamp = LocalDateTime.parse("2022-02-23T10:23:13"),
                auditUserId = "TREV.MACK",
                auditModuleName = "OCDALERT",
                auditClientUserId = "trev.nack",
                auditClientIpAddress = "10.1.1.23",
                auditClientWorkstationName = "MMD1234J",
                auditAdditionalInfo = "POST /api/bookings/2904199/alert",
              )
            }
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.delete(prisoner)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoner/booking-id/$bookingId/alerts/$activeAlertSequence")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoner/booking-id/$bookingId/alerts/$activeAlertSequence")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoner/booking-id/$bookingId/alerts/$activeAlertSequence")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when prisoner's booking not found`() {
        webTestClient.get().uri("/prisoner/booking-id/9999/alerts/$activeAlertSequence")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 404 when prisoner's alert not found`() {
        webTestClient.get().uri("/prisoner/booking-id/$bookingId/alerts/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returned data for a minimal active alert`() {
        webTestClient.get().uri("/prisoner/booking-id/$bookingId/alerts/$activeAlertSequence")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("alertSequence").isEqualTo(activeAlertSequence)
          .jsonPath("alertCode.code").isEqualTo("HPI")
          .jsonPath("alertCode.description").isEqualTo("High Public Interest")
          .jsonPath("type.code").isEqualTo("X")
          .jsonPath("type.description").isEqualTo("Security")
          .jsonPath("date").isEqualTo("2023-07-19")
          .jsonPath("expiryDate").doesNotExist()
          .jsonPath("isActive").isEqualTo(true)
          .jsonPath("isVerified").isEqualTo(false)
          .jsonPath("authorisedBy").doesNotExist()
          .jsonPath("comment").doesNotExist()
      }

      @Test
      fun `returned data for a an inactive alert`() {
        webTestClient.get().uri("/prisoner/booking-id/$bookingId/alerts/$inactiveAlertSequence")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("alertSequence").isEqualTo(inactiveAlertSequence)
          .jsonPath("alertCode.code").isEqualTo("SC")
          .jsonPath("alertCode.description").isEqualTo("Risk to Children")
          .jsonPath("type.code").isEqualTo("S")
          .jsonPath("type.description").isEqualTo("Sexual Offence")
          .jsonPath("date").isEqualTo("2020-07-19")
          .jsonPath("expiryDate").isEqualTo("2025-07-19")
          .jsonPath("isActive").isEqualTo(false)
          .jsonPath("isVerified").isEqualTo(true)
          .jsonPath("authorisedBy").isEqualTo("Security Team")
          .jsonPath("comment").isEqualTo("At risk")
      }

      @Test
      fun `can read audit data related to the alert`() {
        webTestClient.get().uri("/prisoner/booking-id/$bookingId/alerts/$alertSequenceWithAuditMinimal")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("alertSequence").isEqualTo(alertSequenceWithAuditMinimal)
          .jsonPath("audit.createDatetime").exists()
          .jsonPath("audit.createUsername").exists()
          .jsonPath("audit.modifyUserId").doesNotExist()
          .jsonPath("audit.modifyDatetime").doesNotExist()
          .jsonPath("audit.auditTimestamp").exists()
          .jsonPath("audit.auditUserId").exists()
          .jsonPath("audit.auditModuleName").exists()
          .jsonPath("audit.auditClientUserId").exists()
          .jsonPath("audit.auditClientIpAddress").exists()
          .jsonPath("audit.auditClientWorkstationName").exists()
          .jsonPath("audit.auditAdditionalInfo").doesNotExist()
        webTestClient.get().uri("/prisoner/booking-id/$bookingId/alerts/$alertSequenceWithAudit")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("alertSequence").isEqualTo(alertSequenceWithAudit)
          .jsonPath("audit.createDatetime").isEqualTo("2020-01-23T10:23:00")
          .jsonPath("audit.createUsername").isEqualTo("JANE.NARK")
          .jsonPath("audit.modifyUserId").isEqualTo("TREV.NACK")
          .jsonPath("audit.modifyDatetime").isEqualTo("2022-02-23T10:23:12")
          .jsonPath("audit.auditTimestamp").isEqualTo("2022-02-23T10:23:13")
          .jsonPath("audit.auditUserId").isEqualTo("TREV.MACK")
          .jsonPath("audit.auditModuleName").isEqualTo("OCDALERT")
          .jsonPath("audit.auditClientUserId").isEqualTo("trev.nack")
          .jsonPath("audit.auditClientIpAddress").isEqualTo("10.1.1.23")
          .jsonPath("audit.auditClientWorkstationName").isEqualTo("MMD1234J")
          .jsonPath("audit.auditAdditionalInfo").isEqualTo("POST /api/bookings/2904199/alert")
      }
    }
  }
}
