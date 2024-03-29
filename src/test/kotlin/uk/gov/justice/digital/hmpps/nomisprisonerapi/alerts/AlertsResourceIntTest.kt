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
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.ACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.INACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowAction.Companion.DATA_ENTRY
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowAction.Companion.MODIFIED
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WorkFlowAction.Companion.VERIFICATION
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
        staff(firstName = "JANE", lastName = "SMITH") {
          account(username = "JSMITH_GEN", type = "GENERAL")
          account(username = "JSMITH_ADM", type = "ADMIN")
        }
        staff(firstName = "ANALA", lastName = "KASHVI") {
          account(username = "AKASHVI")
        }
        prisoner = offender(nomsId = "A1234AB") {
          bookingId = booking {
            alert(
              createUsername = "SYS",
              modifyUsername = "AKASHVI",
            )
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
              createUsername = "JSMITH_ADM",
            ) {
              workFlowLog(workActionCode = MODIFIED, workFlowStatus = DONE)
              workFlowLog(workActionCode = VERIFICATION, workFlowStatus = COMP)
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
          assertThat(createUsername).isEqualTo("SYS")
          assertThat(createStaffUserAccount).isNull()
          assertThat(modifyUserId).isEqualTo("AKASHVI")
          assertThat(modifyStaffUserAccount?.staff?.lastName).isEqualTo("KASHVI")
          assertThat(modifyStaffUserAccount?.staff?.firstName).isEqualTo("ANALA")

          assertThat(workFlows).hasSize(1)
          assertThat(workFlows[0].logs).hasSize(1)
          val log = workFlows[0].logs.first()
          assertThat(log.createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(log.createDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(log.workActionCode.code).isEqualTo("ENT")
          assertThat(log.workActionCode.description).isEqualTo("Data Entry")
          assertThat(log.workFlowStatus).isEqualTo(DONE)
          assertThat(log.locateAgyLoc).isNull()
          assertThat(log.workActionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(log.createUsername).isEqualTo("SYS")
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
          assertThat(createUsername).isEqualTo("JSMITH_ADM")
          assertThat(createStaffUserAccount?.staff?.firstName).isEqualTo("JANE")
          assertThat(createStaffUserAccount?.staff?.lastName).isEqualTo("SMITH")
          assertThat(modifyUserId).isNull()
          assertThat(modifyStaffUserAccount).isNull()

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
        staff(firstName = "JANE", lastName = "NARK") {
          account(username = "JANE.NARK")
        }
        staff(firstName = "TREVOR", lastName = "NACK") {
          account(username = "TREV.NACK")
        }

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
          .jsonPath("audit.createDisplayName").isEqualTo("JANE NARK")
          .jsonPath("audit.modifyUserId").isEqualTo("TREV.NACK")
          .jsonPath("audit.modifyDisplayName").isEqualTo("TREVOR NACK")
          .jsonPath("audit.modifyDatetime").isEqualTo("2022-02-23T10:23:12")
          .jsonPath("audit.auditTimestamp").isEqualTo("2022-02-23T10:23:13")
          .jsonPath("audit.auditUserId").isEqualTo("TREV.MACK")
          .jsonPath("audit.auditModuleName").isEqualTo("OCDALERT")
          .jsonPath("audit.auditClientUserId").isEqualTo("trev.nack")
          .jsonPath("audit.auditClientIpAddress").isEqualTo("10.1.1.23")
          .jsonPath("audit.auditClientWorkstationName").isEqualTo("MMD1234J")
          .jsonPath("audit.auditAdditionalInfo").isEqualTo("POST /api/bookings/2904199/alert")
      }

      @Test
      fun `can use both GET Urls`() {
        webTestClient.get().uri("/prisoner/booking-id/$bookingId/alerts/$alertSequenceWithAuditMinimal")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus()
          .isOk

        webTestClient.get().uri("/prisoners/booking-id/$bookingId/alerts/$alertSequenceWithAuditMinimal")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus()
          .isOk
      }
    }
  }

  @DisplayName("GET /alerts/ids")
  @Nested
  inner class GetAlertIds {
    var bookingId1 = 0L
    var bookingId2 = 0L
    private val alertSequenceFromJuly2020 = 1L
    private val inactiveAlertSequenceFromJuly2020 = 2L
    private val alertSequenceFromToday = 3L
    private val alertSequenceFromJan2020 = 4L
    private val alertSequenceFromFeb2020 = 4L
    private lateinit var prisoner1: Offender
    private lateinit var prisoner2: Offender

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        prisoner1 = offender(nomsId = "A1234AB") {
          bookingId1 = booking {
            alert(
              sequence = alertSequenceFromJuly2020,
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
              sequence = inactiveAlertSequenceFromJuly2020,
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
              sequence = alertSequenceFromToday,
              alertCode = "HPI",
              typeCode = "X",
            ) {
              audit()
            }
            alert(
              sequence = alertSequenceFromJan2020,
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
        prisoner2 = offender(nomsId = "A1234BC") {
          bookingId2 = booking {
            alert(
              sequence = alertSequenceFromJuly2020,
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
              sequence = inactiveAlertSequenceFromJuly2020,
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
              sequence = alertSequenceFromToday,
              alertCode = "HPI",
              typeCode = "X",
            ) {
              audit()
            }
            alert(
              sequence = alertSequenceFromFeb2020,
              alertCode = "HPI",
              typeCode = "X",
            ) {
              audit(
                createUsername = "JANE.NARK",
                createDatetime = LocalDateTime.parse("2020-02-23T10:23"),
                modifyUserId = "TREV.NACK",
                modifyDatetime = LocalDateTime.parse("2022-03-23T10:23:12"),
                auditTimestamp = LocalDateTime.parse("2022-03-23T10:23:13"),
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
      repository.deleteOffenders()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/alerts/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/alerts/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/alerts/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return offender no, bookingId and alert sequence`() {
        webTestClient.get().uri {
          it.path("/alerts/ids")
            .queryParam("fromDate", "2020-01-23")
            .queryParam("toDate", "2020-01-23")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.numberOfElements").isEqualTo(1)
          .jsonPath("$.content[0].bookingId").isEqualTo(bookingId1)
          .jsonPath("$.content[0].alertSequence").isEqualTo(alertSequenceFromJan2020)
          .jsonPath("$.content[0].offenderNo").isEqualTo("A1234AB")
      }

      @Test
      fun `can filter by just from date`() {
        webTestClient.get().uri {
          it.path("/alerts/ids")
            .queryParam("fromDate", "2020-01-24")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.numberOfElements").isEqualTo(7)
      }

      @Test
      fun `can filter by just to date`() {
        webTestClient.get().uri {
          it.path("/alerts/ids")
            .queryParam("toDate", "2020-01-24")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.numberOfElements").isEqualTo(1)
      }

      @Test
      fun `will get all alerts when there is no filter`() {
        webTestClient.get().uri {
          it.path("/alerts/ids")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.numberOfElements").isEqualTo(8)
      }

      @Test
      fun `will order by booking id, sequence ascending`() {
        webTestClient.get().uri {
          it.path("/alerts/ids")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.numberOfElements").isEqualTo(8)
          .jsonPath("$.content[0].bookingId").isEqualTo(bookingId1)
          .jsonPath("$.content[0].alertSequence").isEqualTo(1)
          .jsonPath("$.content[0].offenderNo").isEqualTo("A1234AB")
          .jsonPath("$.content[1].bookingId").isEqualTo(bookingId1)
          .jsonPath("$.content[1].alertSequence").isEqualTo(2)
          .jsonPath("$.content[1].offenderNo").isEqualTo("A1234AB")
          .jsonPath("$.content[2].bookingId").isEqualTo(bookingId1)
          .jsonPath("$.content[2].alertSequence").isEqualTo(3)
          .jsonPath("$.content[2].offenderNo").isEqualTo("A1234AB")
          .jsonPath("$.content[3].bookingId").isEqualTo(bookingId1)
          .jsonPath("$.content[3].alertSequence").isEqualTo(4)
          .jsonPath("$.content[3].offenderNo").isEqualTo("A1234AB")
          .jsonPath("$.content[4].bookingId").isEqualTo(bookingId2)
          .jsonPath("$.content[4].alertSequence").isEqualTo(1)
          .jsonPath("$.content[4].offenderNo").isEqualTo("A1234BC")
          .jsonPath("$.content[5].bookingId").isEqualTo(bookingId2)
          .jsonPath("$.content[5].alertSequence").isEqualTo(2)
          .jsonPath("$.content[5].offenderNo").isEqualTo("A1234BC")
          .jsonPath("$.content[6].bookingId").isEqualTo(bookingId2)
          .jsonPath("$.content[6].alertSequence").isEqualTo(3)
          .jsonPath("$.content[6].offenderNo").isEqualTo("A1234BC")
          .jsonPath("$.content[7].bookingId").isEqualTo(bookingId2)
          .jsonPath("$.content[7].alertSequence").isEqualTo(4)
          .jsonPath("$.content[7].offenderNo").isEqualTo("A1234BC")
      }
    }
  }

  @DisplayName("DELETE /prisoners/booking-id/{bookingId}/alerts/{alertSequence}")
  @Nested
  inner class DeleteAlert {
    var bookingId = 0L
    private val activeAlertSequence = 1L
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
        webTestClient.delete().uri("/prisoners/booking-id/$bookingId/alerts/$activeAlertSequence")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/prisoners/booking-id/$bookingId/alerts/$activeAlertSequence")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/prisoners/booking-id/$bookingId/alerts/$activeAlertSequence")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class NoValidation {
      @Test
      fun `return 204 when prisoner's booking not found`() {
        webTestClient.delete().uri("/prisoners/booking-id/9999/alerts/$activeAlertSequence")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNoContent
      }

      @Test
      fun `return 204 when prisoner's alert not found`() {
        webTestClient.delete().uri("/prisoners/booking-id/$bookingId/alerts/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the alert`() {
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/alerts/$activeAlertSequence")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus()
          .isOk
        webTestClient.delete().uri("/prisoners/booking-id/$bookingId/alerts/$activeAlertSequence")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus()
          .isNoContent
        webTestClient.get().uri("/prisoners/booking-id/$bookingId/alerts/$activeAlertSequence")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }

  @DisplayName("POST /prisoners/{offenderNo}/alerts")
  @Nested
  inner class CreateAlert {
    private var activeBookingId = 0L
    private lateinit var prisoner: Offender

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        prisoner = offender(nomsId = "A1234AB") {
          activeBookingId = booking {
            alert(
              sequence = 1,
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
              sequence = 2,
              alertCode = "SC",
              typeCode = "S",
              date = LocalDate.parse("2020-07-19"),
              expiryDate = LocalDate.parse("2025-07-19"),
              authorizePersonText = "Security Team",
              verifiedFlag = true,
              status = INACTIVE,
              commentText = "At risk",
            )
          }.bookingId
          booking(bookingBeginDate = LocalDateTime.parse("2021-07-18T10:00:00")) {
            alert(
              sequence = 1,
              alertCode = "SA",
              typeCode = "X",
              date = LocalDate.parse("2021-07-19"),
              expiryDate = null,
              authorizePersonText = null,
              verifiedFlag = false,
              status = ACTIVE,
              commentText = null,
            )
            release(date = LocalDateTime.parse("2021-07-19T10:00:00"))
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.delete(prisoner)
    }

    @Nested
    inner class Security {
      private val validAlert = CreateAlertRequest(
        alertCode = "SA",
        date = LocalDate.now(),
        isActive = true,
        createUsername = "JANE.PEEL",
      )

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validAlert)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validAlert)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validAlert)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private val validAlert = CreateAlertRequest(
        alertCode = "SA",
        date = LocalDate.now(),
        isActive = true,
        createUsername = "JANE.PEEL",
      )

      @Test
      fun `validation fails when prisoner does not exist`() {
        webTestClient.post().uri("/prisoners/A9999ZZ/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validAlert)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `validation fails when alert code is not present`() {
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "date": "2020-01-01",
                "isActive": true,
                "createUsername": "JANE.PEEL"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when create username is not present`() {
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "alertCode": "SA",
                "date": "2020-01-01",
                "isActive": true
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when alert date is not present`() {
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "alertCode": "SA",
                "isActive": true,
                "createUsername": "JANE.PEEL"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when alert code is not valid`() {
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "alertCode": "ZZ",
                "isActive": true,
                "date": "2020-01-01",
                "createUsername": "JANE.PEEL"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `attempt to add the same active alert twice is rejected`() {
        // HPI is already active so will be rejected
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "alertCode": "HPI",
                "isActive": true,
                "date": "2020-01-01",
                "createUsername": "JANE.PEEL"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isEqualTo(409)

        // Can create another inactive HPI alert
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "alertCode": "HPI",
                "isActive": false,
                "date": "2020-01-01",
                "createUsername": "JANE.PEEL"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isEqualTo(201)

        // existing SC is inactive so can create an active one
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "alertCode": "SC",
                "isActive": true,
                "date": "2020-01-01",
                "createUsername": "JANE.PEEL"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isEqualTo(201)

        // SA is active but on a previous booking
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "alertCode": "SA",
                "isActive": true,
                "date": "2020-01-01",
                "createUsername": "JANE.PEEL"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isEqualTo(201)
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `creating an alert with minimal data will return basic data`() {
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "alertCode": "SC",
                "isActive": true,
                "date": "2020-01-01",
                "createUsername": "JANE.PEEL"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isEqualTo(201)
          .expectBody()
          .jsonPath("bookingId").isEqualTo(activeBookingId)
          .jsonPath("alertSequence").isEqualTo(3)
          .jsonPath("alertCode.code").isEqualTo("SC")
          .jsonPath("alertCode.description").isEqualTo("Risk to Children")
          .jsonPath("type.code").isEqualTo("S")
          .jsonPath("type.description").isEqualTo("Sexual Offence")
      }

      @Test
      fun `creating an alert will allow the data to be retrieved`() {
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "alertCode": "SC",
                "isActive": true,
                "date": "2020-01-01",
                "createUsername": "JANE.PEEL"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isEqualTo(201)

        repository.runInTransaction {
          val booking = offenderBookingRepository.findByIdOrNull(activeBookingId)
          assertThat(booking?.alerts).hasSize(3)
          val newAlert = booking?.alerts?.first { it.id.sequence == 3L }!!

          assertThat(newAlert.alertCode.code).isEqualTo("SC")
          assertThat(newAlert.alertType.code).isEqualTo("S")
          assertThat(newAlert.alertDate).isEqualTo("2020-01-01")
          assertThat(newAlert.alertStatus).isEqualTo(ACTIVE)
          assertThat(newAlert.workFlows).hasSize(1)
          assertThat(newAlert.workFlows.first().createUsername).isEqualTo("JANE.PEEL")
          assertThat(newAlert.workFlows.first().createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(newAlert.workFlows.first().logs).hasSize(1)
          assertThat(newAlert.workFlows.first().logs.first().createUsername).isEqualTo("JANE.PEEL")
          assertThat(newAlert.workFlows.first().logs.first().createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(newAlert.workFlows.first().logs.first().workActionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(newAlert.workFlows.first().logs.first().workFlowStatus).isEqualTo(DONE)
          assertThat(newAlert.workFlows.first().logs.first().workActionCode.code).isEqualTo(DATA_ENTRY)
          assertThat(newAlert.createUsername).isEqualTo("JANE.PEEL")
          assertThat(newAlert.createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(newAlert.commentText).isNull()
          assertThat(newAlert.expiryDate).isNull()
          assertThat(newAlert.verifiedFlag).isFalse()
        }
      }

      @Test
      fun `can create an alert that is inactive`() {
        webTestClient.post().uri("/prisoners/A1234AB/alerts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "alertCode": "SC",
                "isActive": false,
                "date": "2020-01-01",
                "expiryDate": "2021-01-01",
                "createUsername": "JANE.PEEL",
                "comment": "Risky",
                "authorisedBy": "Security team"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isEqualTo(201)

        repository.runInTransaction {
          val booking = offenderBookingRepository.findByIdOrNull(activeBookingId)
          assertThat(booking?.alerts).hasSize(3)
          val newAlert = booking?.alerts?.first { it.id.sequence == 3L }!!

          assertThat(newAlert.alertCode.code).isEqualTo("SC")
          assertThat(newAlert.alertType.code).isEqualTo("S")
          assertThat(newAlert.alertDate).isEqualTo("2020-01-01")
          assertThat(newAlert.alertStatus).isEqualTo(INACTIVE)
          assertThat(newAlert.commentText).isEqualTo("Risky")
          assertThat(newAlert.expiryDate).isEqualTo("2021-01-01")
        }
      }
    }
  }

  @DisplayName("PUT /prisoners/booking-id/{bookingId}/alerts/{alertSequence}")
  @Nested
  inner class UpdateAlert {
    private var activeBookingId = 0L
    private var inactiveBookingId = 0L
    private lateinit var prisoner: Offender

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        prisoner = offender(nomsId = "A1234AB") {
          activeBookingId = booking {
            alert(
              sequence = 1,
              alertCode = "HPI",
              typeCode = "X",
              date = LocalDate.parse("2023-07-19"),
              expiryDate = null,
              authorizePersonText = null,
              verifiedFlag = false,
              status = ACTIVE,
              commentText = null,
              createUsername = "BOBBY.BEANS",
            )
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
            )
          }.bookingId
          inactiveBookingId = booking(bookingBeginDate = LocalDateTime.parse("2021-07-18T10:00:00")) {
            alert(
              sequence = 1,
              alertCode = "SA",
              typeCode = "X",
              date = LocalDate.parse("2021-07-19"),
              expiryDate = null,
              authorizePersonText = null,
              verifiedFlag = false,
              status = ACTIVE,
              commentText = null,
            )
            release(date = LocalDateTime.parse("2021-07-19T10:00:00"))
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
      private val validUpdate = UpdateAlertRequest(
        expiryDate = LocalDate.now().plusYears(1),
        date = LocalDate.now(),
        isActive = true,
        updateUsername = "JANE.PEEL",
      )

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/prisoners/booking-id/$activeBookingId/alerts/1")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validUpdate)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/prisoners/booking-id/$activeBookingId/alerts/1")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validUpdate)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/prisoners/booking-id/$activeBookingId/alerts/1")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validUpdate)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private val validUpdate = UpdateAlertRequest(
        expiryDate = LocalDate.now().plusYears(1),
        date = LocalDate.now(),
        isActive = true,
        updateUsername = "JANE.PEEL",
      )

      @Test
      fun `validation fails when booking or alert sequence does not exist`() {
        webTestClient.put().uri("/prisoners/booking-id/$activeBookingId/alerts/99")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validUpdate)
          .exchange()
          .expectStatus().isNotFound
        webTestClient.put().uri("/prisoners/booking-id/999999/alerts/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validUpdate)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `validation fails when update username is not present`() {
        webTestClient.put().uri("/prisoners/booking-id/$activeBookingId/alerts/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "date": "2020-01-01",
                "expiryDate": "2030-01-01",
                "isActive": false
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when alert date is not present`() {
        webTestClient.put().uri("/prisoners/booking-id/$activeBookingId/alerts/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "expiryDate": "2030-01-01",
                "isActive": false,
                "updateUsername": "JANE.PEEL"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `Can update the alert expiry date and status`() {
        webTestClient.put().uri("/prisoners/booking-id/$activeBookingId/alerts/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            UpdateAlertRequest(
              expiryDate = LocalDate.parse("2024-02-28"),
              date = LocalDate.parse("2023-07-19"),
              isActive = false,
              updateUsername = "JANE.PEEL",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody()
          .jsonPath("expiryDate").isEqualTo("2024-02-28")
          .jsonPath("isActive").isEqualTo(false)
          .jsonPath("bookingId").isEqualTo(activeBookingId)
          .jsonPath("alertSequence").isEqualTo(1)
          .jsonPath("alertCode.code").isEqualTo("HPI")
          .jsonPath("type.code").isEqualTo("X")
      }

      @Test
      fun `Can update the alert and retrieve those alert updates`() {
        webTestClient.put().uri("/prisoners/booking-id/$activeBookingId/alerts/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            UpdateAlertRequest(
              expiryDate = LocalDate.parse("2024-02-28"),
              date = LocalDate.parse("2023-07-19"),
              isActive = false,
              updateUsername = "JANE.PEEL",
              comment = "Updated for good reason",
              authorisedBy = "Rasheed in security",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(200)

        repository.runInTransaction {
          val booking = offenderBookingRepository.findByIdOrNull(activeBookingId)
          assertThat(booking?.alerts).hasSize(2)
          val newAlert = booking?.alerts?.first { it.id.sequence == 1L }!!

          assertThat(newAlert.commentText).isEqualTo("Updated for good reason")
          assertThat(newAlert.authorizePersonText).isEqualTo("Rasheed in security")
          assertThat(newAlert.expiryDate).isEqualTo(LocalDate.parse("2024-02-28"))
          assertThat(newAlert.alertDate).isEqualTo("2023-07-19")
          assertThat(newAlert.modifyUserId).isEqualTo("JANE.PEEL")
          assertThat(newAlert.modifyDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))

          // unchanged
          assertThat(newAlert.alertCode.code).isEqualTo("HPI")
          assertThat(newAlert.alertType.code).isEqualTo("X")
          assertThat(newAlert.alertStatus).isEqualTo(INACTIVE)
          assertThat(newAlert.createUsername).isEqualTo("BOBBY.BEANS")
          assertThat(newAlert.verifiedFlag).isFalse()
        }
      }

      @Test
      fun `Update will create a new workflow log modification record`() {
        webTestClient.put().uri("/prisoners/booking-id/$activeBookingId/alerts/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            UpdateAlertRequest(
              expiryDate = LocalDate.parse("2024-02-28"),
              date = LocalDate.parse("2023-07-19"),
              isActive = false,
              updateUsername = "JANE.PEEL",
              comment = "Updated for good reason",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(200)

        repository.runInTransaction {
          val booking = offenderBookingRepository.findByIdOrNull(activeBookingId)
          assertThat(booking?.alerts).hasSize(2)
          val newAlert = booking?.alerts?.first { it.id.sequence == 1L }!!

          assertThat(newAlert.workFlows).hasSize(1)
          assertThat(newAlert.workFlows.first().createUsername).isEqualTo("BOBBY.BEANS")
          assertThat(newAlert.workFlows.first().logs).hasSize(2)
          assertThat(newAlert.workFlows.first().logs[1].createUsername).isEqualTo("JANE.PEEL")
          assertThat(newAlert.workFlows.first().logs[1].createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(newAlert.workFlows.first().logs[1].workActionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(newAlert.workFlows.first().logs[1].workFlowStatus).isEqualTo(DONE)
          assertThat(newAlert.workFlows.first().logs[1].workActionCode.code).isEqualTo(MODIFIED)
        }
      }

      @Test
      fun `Can update the alert on previous booking though this is not expected to ever happen in DPS`() {
        webTestClient.put().uri("/prisoners/booking-id/$inactiveBookingId/alerts/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ALERTS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            UpdateAlertRequest(
              expiryDate = LocalDate.parse("2024-02-28"),
              date = LocalDate.parse("2023-07-19"),
              isActive = false,
              updateUsername = "JANE.PEEL",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(200)
      }
    }
  }
}
