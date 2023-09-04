package uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.time.LocalDate

class NonAssociationsResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  lateinit var offenderAtMoorlands: Offender
  lateinit var offenderAtLeeds: Offender
  lateinit var offenderAtShrewsbury: Offender

  @BeforeEach
  internal fun createPrisoners() {
    nomisDataBuilder.build {
      offenderAtMoorlands =
        offender(nomsId = "A1234TT") {
          booking(agencyLocationId = "MDI")
        }
      offenderAtLeeds =
        offender(nomsId = "A1234TU") {
          booking(agencyLocationId = "LEI")
        }
      offenderAtShrewsbury =
        offender(nomsId = "A1234TV") {
          booking(agencyLocationId = "SYI")
        }
    }
  }

  @AfterEach
  internal fun deleteData() {
    repository.deleteAllNonAssociations()

    repository.delete(offenderAtMoorlands)
    repository.delete(offenderAtLeeds)
    repository.delete(offenderAtShrewsbury)
  }

  @Nested
  inner class CreateNonAssociation {

    private val createNonAssociationRequest: () -> CreateNonAssociationRequest = {
      CreateNonAssociationRequest(
        offenderNo = offenderAtMoorlands.nomsId,
        nsOffenderNo = offenderAtLeeds.nomsId,
        reason = "RIV",
        recipReason = "PER",
        type = "WING",
        authorisedBy = "me!",
        effectiveDate = LocalDate.parse("2023-07-27"),
        comment = "this is a test!",
      )
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/non-associations")
        .body(BodyInserters.fromValue(createNonAssociationRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createNonAssociationRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `offender not found`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(offenderNo = "A0000AA")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Offender with nomsId=A0000AA not found")
        }
    }

    @Test
    fun `nsOffender not found`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(nsOffenderNo = "A0000AA")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("NS Offender with nomsId=A0000AA not found")
        }
    }

    @Test
    fun `invalid reason`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(reason = "invalid")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Reason with code=invalid does not exist")
        }
    }

    @Test
    fun `invalid reciprocal reason`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(recipReason = "invalid")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Reciprocal reason with code=invalid does not exist")
        }
    }

    @Test
    fun `invalid type`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(type = "INVALID")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Type with code=INVALID does not exist")
        }
    }

    @Test
    fun `effectiveDate in the future`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(
          BodyInserters.fromValue(
            createNonAssociationRequest().copy(
              effectiveDate = LocalDate.now().plusDays(1),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Effective date must not be in the future")
        }
    }

    @Test
    fun `comment too long`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(comment = "x".repeat(241))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Comment is too long (max allowed 240 characters)")
        }
    }

    @Test
    fun `invalid date should return bad request`() {
      val invalidSchedule = """{
                "offenderNo"    : "${offenderAtMoorlands.nomsId}",
                "nsOffenderNo"  : "${offenderAtLeeds.nomsId}",
                "reason"        : "RIV",
                "recipReason"   : "PER",
                "type"          : "WING",
                "authorisedBy"  : "me!",
                "effectiveDate" : "2023-13-27", // INVALID DATE
                "comment"       : "this is a test!"
              }
      """.trimIndent()
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(invalidSchedule))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Invalid value for MonthOfYear (valid values 1 - 12): 13")
        }
    }

    @Test
    fun `will create non-association with correct details`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                    "offenderNo"    : "${offenderAtMoorlands.nomsId}",
                    "nsOffenderNo"  : "${offenderAtLeeds.nomsId}",
                    "reason"        : "RIV",
                    "recipReason"   : "PER",
                    "type"          : "WING",
                    "authorisedBy"  : "me!",
                    "effectiveDate" : "2023-02-27",
                    "comment"       : "this is a test!"
                  }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.typeSequence").isEqualTo("1")

      // Check the database
      repository.getNonAssociation(offenderAtMoorlands, offenderAtLeeds).apply {
        assertThat(id.offender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(id.nsOffender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(offenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nsOffenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nonAssociationReason?.code).isEqualTo("PER")
        assertThat(recipNonAssociationReason?.code).isEqualTo("PER")
        val nd = offenderNonAssociationDetails.first()
        assertThat(nd.id.offender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(nd.id.nsOffender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(nd.id.typeSequence).isEqualTo(1)
        assertThat(nd.offenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nd.nsOffenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nd.nonAssociationReason.code).isEqualTo("RIV")
        assertThat(nd.recipNonAssociationReason?.code).isNull()
        assertThat(nd.effectiveDate).isEqualTo(LocalDate.parse("2023-02-27"))
        assertThat(nd.nonAssociationType.code).isEqualTo("WING")
        assertThat(nd.authorisedBy).isEqualTo("me!")
        assertThat(nd.comment).isEqualTo("this is a test!")
        assertThat(nd.nonAssociation).isEqualTo(this)
      }
      repository.getNonAssociation(offenderAtLeeds, offenderAtMoorlands).apply {
        assertThat(id.offender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(id.nsOffender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(offenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nsOffenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nonAssociationReason?.code).isEqualTo("PER")
        assertThat(recipNonAssociationReason?.code).isEqualTo("RIV")
        val nd = offenderNonAssociationDetails.first()
        assertThat(nd.id.offender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(nd.id.nsOffender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(nd.id.typeSequence).isEqualTo(1)
        assertThat(nd.offenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nd.nsOffenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nd.nonAssociationReason.code).isEqualTo("PER")
        assertThat(nd.recipNonAssociationReason?.code).isNull()
        assertThat(nd.effectiveDate).isEqualTo(LocalDate.parse("2023-02-27"))
        assertThat(nd.nonAssociationType.code).isEqualTo("WING")
        assertThat(nd.authorisedBy).isEqualTo("me!")
        assertThat(nd.comment).isEqualTo("this is a test!")
        assertThat(nd.nonAssociation).isEqualTo(this)
      }
    }

    @Test
    fun `will create non-association where parent record already exists`() {
      nomisDataBuilder.build {
        nonAssociation(offenderAtMoorlands, offenderAtLeeds) {
          nonAssociationDetail(
            typeSeq = 1,
            nonAssociationReason = "PER",
            effectiveDate = LocalDate.parse("2020-01-01"),
            expiryDate = LocalDate.parse("2020-01-31"),
            nonAssociationType = "WING",
            authorisedBy = "Staff Member 1",
            comment = "this is a closed NA",
          )
        }
        nonAssociation(offenderAtLeeds, offenderAtMoorlands) {
          nonAssociationDetail(
            typeSeq = 1,
            nonAssociationReason = "VIC",
            effectiveDate = LocalDate.parse("2020-01-01"),
            expiryDate = LocalDate.parse("2020-01-31"),
            nonAssociationType = "WING",
            authorisedBy = "Staff Member 1",
            comment = "this is a closed NA",
          )
        }
      }
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                    "offenderNo"    : "${offenderAtMoorlands.nomsId}",
                    "nsOffenderNo"  : "${offenderAtLeeds.nomsId}",
                    "reason"        : "RIV",
                    "recipReason"   : "PER",
                    "type"          : "WING",
                    "authorisedBy"  : "me!",
                    "effectiveDate" : "2023-02-27",
                    "comment"       : "this is a test!"
                  }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.typeSequence").isEqualTo("2")

      // Check the database
      repository.getNonAssociation(offenderAtMoorlands, offenderAtLeeds).apply {
        assertThat(id.offender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(id.nsOffender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(offenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nsOffenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nonAssociationReason?.code).isEqualTo("PER")
        assertThat(recipNonAssociationReason?.code).isEqualTo("PER")

        val existing = offenderNonAssociationDetails.first()
        assertThat(existing.id.typeSequence).isEqualTo(1)
        assertThat(existing.expiryDate).isEqualTo(LocalDate.parse("2020-01-31"))

        val nd = offenderNonAssociationDetails.last()
        assertThat(nd.id.offender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(nd.id.nsOffender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(nd.id.typeSequence).isEqualTo(2)
        assertThat(nd.offenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nd.nsOffenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nd.nonAssociationReason.code).isEqualTo("RIV")
        assertThat(nd.recipNonAssociationReason?.code).isNull()
        assertThat(nd.effectiveDate).isEqualTo(LocalDate.parse("2023-02-27"))
        assertThat(nd.nonAssociationType.code).isEqualTo("WING")
        assertThat(nd.authorisedBy).isEqualTo("me!")
        assertThat(nd.comment).isEqualTo("this is a test!")
        assertThat(nd.nonAssociation).isEqualTo(this)
      }
      repository.getNonAssociation(offenderAtLeeds, offenderAtMoorlands).apply {
        assertThat(id.offender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(id.nsOffender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(offenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nsOffenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nonAssociationReason?.code).isEqualTo("PER")
        assertThat(recipNonAssociationReason?.code).isEqualTo("RIV")

        val existing = offenderNonAssociationDetails.first()
        assertThat(existing.id.typeSequence).isEqualTo(1)
        assertThat(existing.expiryDate).isEqualTo(LocalDate.parse("2020-01-31"))

        val nd = offenderNonAssociationDetails.last()
        assertThat(nd.id.offender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(nd.id.nsOffender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(nd.id.typeSequence).isEqualTo(2)
        assertThat(nd.offenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nd.nsOffenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nd.nonAssociationReason.code).isEqualTo("PER")
        assertThat(nd.recipNonAssociationReason?.code).isNull()
        assertThat(nd.effectiveDate).isEqualTo(LocalDate.parse("2023-02-27"))
        assertThat(nd.nonAssociationType.code).isEqualTo("WING")
        assertThat(nd.authorisedBy).isEqualTo("me!")
        assertThat(nd.comment).isEqualTo("this is a test!")
        assertThat(nd.nonAssociation).isEqualTo(this)
      }
    }
  }

  @Nested
  inner class UpdateNonAssociation {

    private val updateNonAssociationRequest: () -> UpdateNonAssociationRequest = {
      UpdateNonAssociationRequest(
        reason = "BUL",
        recipReason = "VIC",
        type = "LAND",
        authorisedBy = "me!",
        effectiveDate = LocalDate.parse("2023-07-30"),
        comment = "this is a modified test!",
      )
    }

    @BeforeEach
    internal fun createNonAssociation() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                    "offenderNo"    : "${offenderAtMoorlands.nomsId}",
                    "nsOffenderNo"  : "${offenderAtLeeds.nomsId}",
                    "reason"        : "RIV",
                    "recipReason"   : "PER",
                    "type"          : "WING",
                    "authorisedBy"  : "me!",
                    "effectiveDate" : "2023-02-27",
                    "comment"       : "this is a test!"
                  }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.typeSequence").isEqualTo("1")
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put()
        .uri("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}", "A1234AA", "A1234AA")
        .body(BodyInserters.fromValue(updateNonAssociationRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put()
        .uri("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}", "A1234AA", "A1234AA")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(updateNonAssociationRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put()
        .uri("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}", "A1234AA", "A1234AA")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `non-association does not exist`() {
      webTestClient.put()
        .uri("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}", "A1234AA", "A1234AA")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest()))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `invalid reason`() {
      webTestClient.put().uri(
        "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}",
        offenderAtMoorlands.nomsId,
        offenderAtLeeds.nomsId,
      )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest().copy(reason = "invalid")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Reason with code=invalid does not exist")
        }
    }

    @Test
    fun `invalid reciprocal reason`() {
      webTestClient.put().uri(
        "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}",
        offenderAtMoorlands.nomsId,
        offenderAtLeeds.nomsId,
      )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest().copy(recipReason = "invalid")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Reciprocal reason with code=invalid does not exist")
        }
    }

    @Test
    fun `invalid type`() {
      webTestClient.put().uri(
        "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}",
        offenderAtMoorlands.nomsId,
        offenderAtLeeds.nomsId,
      )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest().copy(type = "INVALID")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Type with code=INVALID does not exist")
        }
    }

    @Test
    fun `effectiveDate in the future`() {
      webTestClient.put().uri(
        "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}",
        offenderAtMoorlands.nomsId,
        offenderAtLeeds.nomsId,
      )
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(
          BodyInserters.fromValue(
            updateNonAssociationRequest().copy(
              effectiveDate = LocalDate.now().plusDays(1),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Effective date must not be in the future")
        }
    }

    @Test
    fun `comment too long`() {
      webTestClient.put().uri(
        "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}",
        offenderAtMoorlands.nomsId,
        offenderAtLeeds.nomsId,
      )
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest().copy(comment = "x".repeat(241))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Comment is too long (max allowed 240 characters)")
        }
    }

    @Test
    fun `invalid date should return bad request`() {
      val invalidSchedule = """{
              "reason"        : "BUL",
              "recipReason"   : "VIC",
              "type"          : "LAND",
              "authorisedBy"  : "Joe Bloggs",
              "effectiveDate" : "2023-13-27", // INVALID DATE
              "comment"       : "this is a modified test!"
            }
      """.trimIndent()
      webTestClient.put().uri(
        "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}",
        offenderAtMoorlands.nomsId,
        offenderAtLeeds.nomsId,
      )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(invalidSchedule))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Invalid value for MonthOfYear (valid values 1 - 12): 13")
        }
    }

    @Test
    fun `will update non-association with correct details`() {
      webTestClient.put()
        .uri(
          "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}",
          offenderAtMoorlands.nomsId,
          offenderAtLeeds.nomsId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                  "reason"        : "BUL",
                  "recipReason"   : "VIC",
                  "type"          : "LAND",
                  "authorisedBy"  : "Joe Bloggs",
                  "effectiveDate" : "2023-02-28",
                  "comment"       : "this is a modified test!"
                }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isOk

      // Check the database
      repository.getNonAssociation(offenderAtMoorlands, offenderAtLeeds).apply {
        assertThat(id.offender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(id.nsOffender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(offenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nsOffenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nonAssociationReason?.code).isEqualTo("VIC")
        assertThat(recipNonAssociationReason?.code).isEqualTo("VIC")
        val nd = offenderNonAssociationDetails.first()
        assertThat(nd.id.offender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(nd.id.nsOffender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(nd.id.typeSequence).isEqualTo(1)
        assertThat(nd.offenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nd.nsOffenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nd.nonAssociationReason.code).isEqualTo("BUL")
        assertThat(nd.recipNonAssociationReason?.code).isNull()
        assertThat(nd.effectiveDate).isEqualTo(LocalDate.parse("2023-02-28"))
        assertThat(nd.nonAssociationType.code).isEqualTo("LAND")
        assertThat(nd.authorisedBy).isEqualTo("Joe Bloggs")
        assertThat(nd.comment).isEqualTo("this is a modified test!")
        assertThat(nd.nonAssociation).isEqualTo(this)
      }
      repository.getNonAssociation(offenderAtLeeds, offenderAtMoorlands).apply {
        assertThat(id.offender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(id.nsOffender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(offenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nsOffenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nonAssociationReason?.code).isEqualTo("VIC")
        assertThat(recipNonAssociationReason?.code).isEqualTo("BUL")
        val nd = offenderNonAssociationDetails.first()
        assertThat(nd.id.offender.nomsId).isEqualTo(offenderAtLeeds.nomsId)
        assertThat(nd.id.nsOffender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(nd.id.typeSequence).isEqualTo(1)
        assertThat(nd.offenderBooking.bookingId).isEqualTo(offenderAtLeeds.latestBooking().bookingId)
        assertThat(nd.nsOffenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nd.nonAssociationReason.code).isEqualTo("VIC")
        assertThat(nd.recipNonAssociationReason?.code).isNull()
        assertThat(nd.effectiveDate).isEqualTo(LocalDate.parse("2023-02-28"))
        assertThat(nd.nonAssociationType.code).isEqualTo("LAND")
        assertThat(nd.authorisedBy).isEqualTo("Joe Bloggs")
        assertThat(nd.comment).isEqualTo("this is a modified test!")
        assertThat(nd.nonAssociation).isEqualTo(this)
      }
    }
  }

  @Nested
  inner class CloseNonAssociation {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put()
        .uri("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/close", "A1234AA", "A1234AA")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put()
        .uri("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/close", "A1234AA", "A1234AA")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put()
        .uri("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/close", "A1234AA", "A1234AA")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `non-association does not exist`() {
      webTestClient.put()
        .uri("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/close", "A1234AA", "A1234AA")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `will close non-association correctly`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
                    "offenderNo"    : "${offenderAtMoorlands.nomsId}",
                    "nsOffenderNo"  : "${offenderAtLeeds.nomsId}",
                    "reason"        : "RIV",
                    "recipReason"   : "PER",
                    "type"          : "WING",
                    "authorisedBy"  : "me!",
                    "effectiveDate" : "2023-02-27",
                    "comment"       : "this is a test!"
                  }
            """.trimIndent(),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.put()
        .uri(
          "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/close",
          offenderAtMoorlands.nomsId,
          offenderAtLeeds.nomsId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk

      // Check the database
      val na = repository.getNonAssociation(offenderAtMoorlands, offenderAtLeeds)

      assertThat(na.offenderNonAssociationDetails.first().expiryDate).isEqualTo(LocalDate.now())
    }
  }

  @Nested
  inner class GetSpecificNonAssociationById {

    @BeforeEach
    internal fun createNonAssociations() {
      nomisDataBuilder.build {
        nonAssociation(offenderAtMoorlands, offenderAtLeeds) {
          nonAssociationDetail(
            typeSeq = 1,
            nonAssociationReason = "BUL",
            effectiveDate = LocalDate.parse("2021-02-28"),
            nonAssociationType = "LAND",
            authorisedBy = "Staff Member",
            comment = "this is a GET test!",
          )
          nonAssociationDetail(
            typeSeq = 2,
            nonAssociationReason = "PER",
            effectiveDate = LocalDate.parse("2020-01-01"),
            expiryDate = LocalDate.parse("2020-01-31"),
            nonAssociationType = "WING",
            authorisedBy = "Staff Member 2",
            comment = "this is a closed NA",
          )
        }
        nonAssociation(offenderAtMoorlands, offenderAtShrewsbury) {
          nonAssociationDetail(
            typeSeq = 1,
            nonAssociationReason = "VIC",
            effectiveDate = LocalDate.parse("2020-01-01"),
            expiryDate = LocalDate.parse("2020-01-31"),
            nonAssociationType = "WING",
            authorisedBy = "Staff Member 3",
            comment = "this is a closed NA",
          )
        }
      }
    }

    @Test
    fun `get open NA by id`() {
      webTestClient
        .get().uri(
          "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}",
          offenderAtMoorlands.nomsId,
          offenderAtLeeds.nomsId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.offenderNo").isEqualTo(offenderAtMoorlands.nomsId)
        .jsonPath("$.nsOffenderNo").isEqualTo(offenderAtLeeds.nomsId)
        .jsonPath("$.typeSequence").isEqualTo("1")
        .jsonPath("$.reason").isEqualTo("BUL")
        .jsonPath("$.recipReason").isEqualTo("VIC")
        .jsonPath("$.type").isEqualTo("LAND")
        .jsonPath("$.authorisedBy").isEqualTo("Staff Member")
        .jsonPath("$.effectiveDate").isEqualTo("2021-02-28")
        .jsonPath("$.expiryDate").doesNotExist()
        .jsonPath("$.comment").isEqualTo("this is a GET test!")
    }

    @Test
    fun `get NA by id and sequence`() {
      webTestClient
        .get().uri(
          "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}?typeSequence=2",
          offenderAtMoorlands.nomsId,
          offenderAtLeeds.nomsId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.offenderNo").isEqualTo(offenderAtMoorlands.nomsId)
        .jsonPath("$.nsOffenderNo").isEqualTo(offenderAtLeeds.nomsId)
        .jsonPath("$.typeSequence").isEqualTo("2")
        .jsonPath("$.comment").isEqualTo("this is a closed NA")
    }

    @Test
    fun `get NA by id and sequence not found`() {
      webTestClient
        .get().uri(
          "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}?typeSequence=4",
          offenderAtMoorlands.nomsId,
          offenderAtLeeds.nomsId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `offender not found`() {
      webTestClient.get().uri("/non-associations/offender/A0000AA/ns-offender/A0000BB")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `non-associations not found`() {
      repository.deleteAllNonAssociations()
      webTestClient.get().uri(
        "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}",
        offenderAtLeeds.nomsId,
        offenderAtMoorlands.nomsId,
      )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `NA exists but no open non-associations`() {
      repository.deleteAllNonAssociations()
      webTestClient.get().uri(
        "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}",
        offenderAtMoorlands.nomsId,
        offenderAtShrewsbury.nomsId,
      )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get non-associations prevents access without appropriate role`() {
      assertThat(
        webTestClient.get()
          .uri("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}", "A1234AA", "A1234AA")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }
  }

  @Nested
  inner class GetAllNonAssociationDetailsById {

    @BeforeEach
    internal fun createNonAssociations() {
      nomisDataBuilder.build {
        nonAssociation(offenderAtMoorlands, offenderAtLeeds) {
          nonAssociationDetail(
            typeSeq = 1,
            nonAssociationReason = "BUL",
            effectiveDate = LocalDate.parse("2021-02-28"),
            nonAssociationType = "LAND",
            authorisedBy = "Staff Member",
            comment = "this is a GET test!",
          )
          nonAssociationDetail(
            typeSeq = 2,
            nonAssociationReason = "PER",
            effectiveDate = LocalDate.parse("2020-01-01"),
            expiryDate = LocalDate.parse("2020-01-31"),
            nonAssociationType = "WING",
            authorisedBy = "Staff Member 2",
            comment = "this is a closed NA",
          )
        }
      }
    }

    @Test
    fun `get all by id`() {
      webTestClient
        .get().uri(
          "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/all",
          offenderAtMoorlands.nomsId,
          offenderAtLeeds.nomsId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].offenderNo").isEqualTo(offenderAtMoorlands.nomsId)
        .jsonPath("$[0].nsOffenderNo").isEqualTo(offenderAtLeeds.nomsId)
        .jsonPath("$[0].typeSequence").isEqualTo("1")
        .jsonPath("$[0].reason").isEqualTo("BUL")
        .jsonPath("$[0].recipReason").isEqualTo("VIC")
        .jsonPath("$[0].type").isEqualTo("LAND")
        .jsonPath("$[0].authorisedBy").isEqualTo("Staff Member")
        .jsonPath("$[0].effectiveDate").isEqualTo("2021-02-28")
        .jsonPath("$[0].expiryDate").doesNotExist()
        .jsonPath("$[0].comment").isEqualTo("this is a GET test!")
        .jsonPath("$[1].offenderNo").isEqualTo(offenderAtMoorlands.nomsId)
        .jsonPath("$[1].nsOffenderNo").isEqualTo(offenderAtLeeds.nomsId)
        .jsonPath("$[1].typeSequence").isEqualTo("2")
        .jsonPath("$[1].reason").isEqualTo("PER")
        .jsonPath("$[1].recipReason").isEqualTo("VIC")
        .jsonPath("$[1].type").isEqualTo("WING")
        .jsonPath("$[1].authorisedBy").isEqualTo("Staff Member 2")
        .jsonPath("$[1].effectiveDate").isEqualTo("2020-01-01")
        .jsonPath("$[1].expiryDate").isEqualTo("2020-01-31")
        .jsonPath("$[1].comment").isEqualTo("this is a closed NA")
    }

    @Test
    fun `offender not found`() {
      webTestClient.get().uri("/non-associations/offender/A0000AA/ns-offender/A0000BB/all")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `non-associations not found`() {
      repository.deleteAllNonAssociations()
      webTestClient.get().uri(
        "/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/all",
        offenderAtLeeds.nomsId,
        offenderAtMoorlands.nomsId,
      )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get non-associations prevents access without appropriate role`() {
      assertThat(
        webTestClient.get()
          .uri("/non-associations/offender/{offenderNo}/ns-offender/{nsOffenderNo}/all", "A1234AA", "A1234AA")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }
  }

  @Nested
  inner class GetNonAssociationIdsByFilterRequest {

    @BeforeEach
    internal fun createNonAssociations() {
      nomisDataBuilder.build {
        nonAssociation(offenderAtMoorlands, offenderAtLeeds) {
          nonAssociationDetail(
            nonAssociationReason = "BUL",
            effectiveDate = LocalDate.parse("2023-01-01"),
            nonAssociationType = "LAND",
          )
        }
        nonAssociation(offenderAtMoorlands, offenderAtShrewsbury) {
          nonAssociationDetail(
            nonAssociationReason = "BUL",
            effectiveDate = LocalDate.parse("2023-01-02"),
            nonAssociationType = "LAND",
          )
        }
        nonAssociation(offenderAtLeeds, offenderAtShrewsbury) {
          nonAssociationDetail(
            nonAssociationReason = "BUL",
            effectiveDate = LocalDate.parse("2023-01-03"),
            nonAssociationType = "LAND",
          )
        }
      }
    }

    @Test
    fun `get all ids - no filter specified`() {
      webTestClient.get().uri("/non-associations/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(3)
    }

    @Test
    fun `get non-associations issued within a given date range 1`() {
      webTestClient.get().uri {
        it.path("/non-associations/ids")
          .queryParam("fromDate", "2000-01-01")
          .queryParam("toDate", "2023-01-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(1)
        .jsonPath("$.content[0].offenderNo1").isEqualTo(offenderAtMoorlands.nomsId)
        .jsonPath("$.content[0].offenderNo2").isEqualTo(offenderAtLeeds.nomsId)
    }

    @Test
    fun `get non-associations issued within a given date range 2`() {
      webTestClient
        .get().uri {
          it.path("/non-associations/ids")
            .queryParam("fromDate", "2023-01-03")
            .queryParam("toDate", "2026-01-01")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(1)
        .jsonPath("$.content[0].offenderNo1").isEqualTo(offenderAtLeeds.nomsId)
        .jsonPath("$.content[0].offenderNo2").isEqualTo(offenderAtShrewsbury.nomsId)
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/non-associations/ids")
          .queryParam("size", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `can request a different page`() {
      webTestClient.get().uri {
        it.path("/non-associations/ids")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `malformed date returns bad request`() {
      webTestClient.get().uri {
        it.path("/non-associations/ids")
          .queryParam("fromDate", "202-10-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `get non-associations prevents access without appropriate role`() {
      assertThat(
        webTestClient.get().uri("/non-associations/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get non-associations prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/non-associations/ids")
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }
}
