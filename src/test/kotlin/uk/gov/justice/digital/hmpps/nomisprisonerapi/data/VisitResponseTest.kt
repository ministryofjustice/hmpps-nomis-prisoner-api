package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.VisitResponse.Visitor
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PersonAddressBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PersonBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import java.time.LocalDate
import java.time.LocalDateTime

internal class VisitResponseTest {
  lateinit var visitor: VisitVisitor
  val visit = Visit(
    id = 99L,
    offenderBooking = anOffenderBooking("A1234LK"),
    visitDate = LocalDate.parse("2020-01-01"),
    startDateTime = LocalDateTime.parse("2020-01-01T10:00:00"),
    endDateTime = LocalDateTime.parse("2020-01-01T11:00:00"),
    visitType = VisitType(code = "SCON", description = "Social Visit"),
    commentText = "Contact Jane 0978 5652727",
    visitorConcernText = "None",
    location = AgencyLocation(id = "LEI", description = "Leeds HMP"),
    agencyInternalLocation = AgencyInternalLocation(
      agencyId = "LEI",
      description = "LEI-VIS-01",
      locationCode = "VIS-01",
      locationType = "VIS"
    ),
    visitStatus = VisitStatus(code = "SCH", description = "Scheduled"),
    visitors = mutableListOf(),
  ).apply {
    this.visitors.add(
      VisitVisitor(
        person = PersonBuilder().build().apply { id = 88L },
        groupLeader = true,
        visit = this
      )
    )
  }

  @DisplayName("constructor")
  @Nested
  inner class Constructor {
    @BeforeEach
    internal fun setUp() {
      visitor = visit.visitors.first()
    }

    @Test
    internal fun `will copy basic information about the visit`() {
      val response = VisitResponse(visit)

      assertThat(response.visitId).isEqualTo(99L)
      assertThat(response.offenderNo).isEqualTo("A1234LK")
      assertThat(response.prisonId).isEqualTo("LEI")
      assertThat(response.startDateTime).isEqualTo("2020-01-01T10:00:00")
      assertThat(response.endDateTime).isEqualTo("2020-01-01T11:00:00")
      assertThat(response.visitType.code).isEqualTo("SCON")
      assertThat(response.visitStatus.code).isEqualTo("SCH")
      assertThat(response.agencyInternalLocation?.description).isEqualTo("LEI-VIS-01")
      assertThat(response.commentText).isEqualTo("Contact Jane 0978 5652727")
      assertThat(response.visitorConcernText).isEqualTo("None")
      assertThat(response.visitors).containsExactly(Visitor(personId = 88L, leadVisitor = true))
    }

    @Test
    internal fun `will have no lead visitor if not present`() {
      val theVisitor = visit.copy(visitors = mutableListOf(visitor.copy(groupLeader = false)))

      assertThat(VisitResponse(theVisitor).leadVisitor).isNull()
    }

    @Test
    internal fun `will have empty phone list if there is none`() {
      val response = VisitResponse(visit)

      assertThat(response.leadVisitor?.telephones).isEmpty()
    }

    @Test
    internal fun `will have single phone in list if exits as a global number`() {
      val theVisitor =
        visit.copy(
          visitors = mutableListOf(
            visitor.copy(
              person = PersonBuilder(
                phoneNumbers = listOf(
                  Triple(
                    "HOME",
                    "0123456789",
                    "ext: 876"
                  )
                )
              ).build()
            )
          )
        )
      val response = VisitResponse(theVisitor)

      assertThat(response.leadVisitor?.telephones).contains("0123456789 ext: 876")
    }

    @Test
    internal fun `will have multiple phone in list if they exist as a global numbers`() {
      val theVisitor =
        visit.copy(
          visitors = mutableListOf(
            visitor.copy(
              person = PersonBuilder(
                phoneNumbers = listOf(
                  Triple("HOME", "0123456789", null),
                  Triple("MOBL", "07973 121212", null)
                )
              ).build()
            )
          )
        )
      val response = VisitResponse(theVisitor)

      assertThat(response.leadVisitor?.telephones).contains("0123456789", "07973 121212")
    }

    @Test
    internal fun `will include numbers from addresses as well global numbers`() {
      val theVisitor =
        visit.copy(
          visitors = mutableListOf(
            visitor.copy(
              person = PersonBuilder(
                phoneNumbers = listOf(
                  Triple("HOME", "0123456789", null),
                  Triple("MOBL", "07973 121212", "x777"),
                ),
                addressBuilders = listOf(
                  PersonAddressBuilder(
                    phoneNumbers = listOf(Triple("HOME", "1234567890", null), Triple("MOBL", "07973 333333", null))
                  ),
                  PersonAddressBuilder(
                    phoneNumbers = listOf(Triple("HOME", "2345678901", "x888"), Triple("MOBL", "07973 444444", null))
                  )
                )
              ).build()
            )
          )
        )
      val response = VisitResponse(theVisitor)

      assertThat(response.leadVisitor?.telephones).contains(
        "0123456789",
        "07973 121212 x777",
        "1234567890",
        "07973 333333",
        "2345678901 x888",
        "07973 444444"
      )
    }

    @Test
    internal fun `will order numbers by last changed`() {
      val theVisitor =
        visit.copy(
          visitors = mutableListOf(
            visitor.copy(
              person = PersonBuilder(
                phoneNumbers = listOf(
                  Triple("HOME", "0123456789", null),
                  Triple("MOBL", "07973 121212", "x777"),
                ),
                addressBuilders = listOf(
                  PersonAddressBuilder(
                    phoneNumbers = listOf(Triple("HOME", "1234567890", null), Triple("MOBL", "07973 333333", null))
                  ),
                  PersonAddressBuilder(
                    phoneNumbers = listOf(Triple("HOME", "2345678901", "x888"), Triple("MOBL", "07973 444444", null))
                  )
                )
              ).build().apply {
                this.phones[0].whenCreated = LocalDateTime.now().minusDays(10) // 6th
                this.phones[1].whenCreated = LocalDateTime.now().minusDays(11)
                this.phones[1].whenModified = LocalDateTime.now().minusDays(4) // 4th
                this.addresses[0].phones[0].whenCreated = LocalDateTime.now().minusDays(8) // 5th
                this.addresses[0].phones[1].whenCreated = LocalDateTime.now().minusDays(9)
                this.addresses[0].phones[1].whenModified = LocalDateTime.now().minusDays(1) // 1st
                this.addresses[1].phones[0].whenCreated = LocalDateTime.now().minusDays(2) // 2nd
                this.addresses[1].phones[1].whenCreated = LocalDateTime.now().minusDays(3) // 3rd
              },
            )
          )
        )
      val response = VisitResponse(theVisitor)

      assertThat(response.leadVisitor?.telephones).containsExactly(
        "07973 333333",
        "2345678901 x888",
        "07973 444444",
        "07973 121212 x777",
        "1234567890",
        "0123456789",
      )
    }
  }
}

fun anOffenderBooking(nomsId: String): OffenderBooking {
  return OffenderBookingBuilder().build(
    offender = OffenderBuilder(nomsId = nomsId).build(Gender("F", "FEMALE")),
    bookingSequence = 1,
    AgencyLocation(id = "LEI", description = "Leeds HMP")
  )
}
