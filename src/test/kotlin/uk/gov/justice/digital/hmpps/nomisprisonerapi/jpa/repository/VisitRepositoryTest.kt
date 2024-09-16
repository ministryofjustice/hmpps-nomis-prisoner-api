package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.LegacyOffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.LegacyPersonBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.LegacyVisitBalanceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SearchLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Visit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitVisitor
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(HmppsAuthenticationHolder::class, AuditorAwareImpl::class, Repository::class)
@WithMockAuthUser
class VisitRepositoryTest {
  @Autowired
  lateinit var builderRepository: Repository

  @Autowired
  lateinit var repository: VisitRepository

  @Autowired
  lateinit var visitVisitorRepository: VisitVisitorRepository

  @Autowired
  lateinit var visitTypeRepository: ReferenceCodeRepository<VisitType>

  @Autowired
  lateinit var visitStatusRepository: ReferenceCodeRepository<VisitStatus>

  @Autowired
  lateinit var searchRepository: ReferenceCodeRepository<SearchLevel>

  @Autowired
  lateinit var agencyRepository: AgencyLocationRepository

  @Autowired
  lateinit var agencyInternalRepository: AgencyInternalLocationRepository

  @Autowired
  lateinit var offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository

  @Autowired
  lateinit var visitOrderAdjustmentReasonRepository: ReferenceCodeRepository<VisitOrderAdjustmentReason>

  @Autowired
  lateinit var entityManager: TestEntityManager

  @Test
  fun saveVisit() {
    val seedOffenderBooking = builderRepository.save(
      LegacyOffenderBuilder()
        .withBooking(OffenderBookingBuilder().withVisitBalance()),
    ).latestBooking()

    val seedPerson1 = builderRepository.save(LegacyPersonBuilder())
    val seedPerson2 = builderRepository.save(LegacyPersonBuilder())

    val visit = Visit(
      offenderBooking = seedOffenderBooking,
      commentText = "comment text",
      visitorConcernText = "visitor concerns",
      visitDate = LocalDate.parse("2009-12-21"),
      startDateTime = LocalDateTime.parse("2009-12-21T13:15"),
      endDateTime = LocalDateTime.parse("2009-12-21T14:15"),
      visitType = visitTypeRepository.findById(VisitType.pk("SCON")).orElseThrow(),
      visitStatus = visitStatusRepository.findById(VisitStatus.pk("SCH")).orElseThrow(),
      searchLevel = searchRepository.findById(SearchLevel.pk("FULL")).orElseThrow(),
      location = agencyRepository.findById("LEI").orElseThrow(),
      agencyInternalLocation = agencyInternalRepository.findById(-3L).orElseThrow(),
    )

    visit.visitors.add(
      VisitVisitor(
        offenderBooking = seedOffenderBooking,
        visit = visit,
        person = seedPerson1,
        assistedVisit = true,
        groupLeader = true,
      ),
    )
    visit.visitors.add(
      VisitVisitor(
        offenderBooking = seedOffenderBooking,
        visit = visit,
        person = seedPerson2,
        eventId = visitVisitorRepository.getEventId(),
      ),
    )

    repository.save(visit)
    entityManager.flush()

    val persistedVisitList = repository.findByOffenderBooking(seedOffenderBooking)
    assertThat(persistedVisitList).isNotEmpty
    val persistedVisit = persistedVisitList[0]

    assertThat(persistedVisit.visitDate).isEqualTo(LocalDate.parse("2009-12-21"))
    assertThat(persistedVisit.startDateTime).isEqualTo(LocalDateTime.parse("2009-12-21T13:15"))
    assertThat(persistedVisit.endDateTime).isEqualTo(LocalDateTime.parse("2009-12-21T14:15"))
    assertThat(persistedVisit.id).isNotNull
    assertThat(persistedVisit.searchLevel!!.description).isEqualTo("Full Search")
    assertThat(persistedVisit.visitStatus.description).isEqualTo("Scheduled")
    assertThat(persistedVisit.visitorConcernText).isEqualTo("visitor concerns")

    val visitVisitors = persistedVisit.visitors
    assertThat(visitVisitors.size).isEqualTo(2)
    val (_, offenderBooking, parentVisit, person, groupLeader, assistedVisit) = visitVisitors[0]
    assertThat(offenderBooking?.bookingId).isEqualTo(seedOffenderBooking.bookingId)
    assertThat(parentVisit.id).isEqualTo(visit.id)
    assertThat(groupLeader).isTrue
    assertThat(assistedVisit).isTrue
    assertThat(person?.id).isEqualTo(seedPerson1.id)
    assertThat(visitVisitors[1].person?.id).isEqualTo(seedPerson2.id)
    assertThat(visitVisitors[1].eventId).isGreaterThan(0)
  }

  @Test
  fun saveBalanceAdjustment() {
    val seedOffenderBooking = builderRepository.save(
      LegacyOffenderBuilder()
        .withBooking(
          OffenderBookingBuilder().withVisitBalance(
            LegacyVisitBalanceBuilder(
              remainingPrivilegedVisitOrders = 2,
              remainingVisitOrders = 25,
            ),
          ),
        ),
    ).latestBooking()
    val seedBalance = seedOffenderBooking.visitBalance ?: throw IllegalStateException("No visit balance")
    assertThat(seedBalance.remainingVisitOrders).isEqualTo(25)
    assertThat(seedBalance.remainingPrivilegedVisitOrders).isEqualTo(2)

    offenderVisitBalanceAdjustmentRepository.save(
      OffenderVisitBalanceAdjustment(
        offenderBooking = seedOffenderBooking,
        adjustDate = LocalDate.of(2020, 12, 21),
        adjustReasonCode = visitOrderAdjustmentReasonRepository.findById(VisitOrderAdjustmentReason.VO_ISSUE)
          .orElseThrow(),
        remainingVisitOrders = -1,
        // from offender_visit_balances
        previousRemainingVisitOrders = seedBalance.remainingVisitOrders,
        commentText = "test comment",
        authorisedStaffId = 123L,
        endorsedStaffId = 123L,
      ),
    )
    entityManager.flush()

    val offenderVisitBalanceAdjustment = offenderVisitBalanceAdjustmentRepository.findAll().first()
    assertThat(offenderVisitBalanceAdjustment.id).isEqualTo(1)
    assertThat(offenderVisitBalanceAdjustment.offenderBooking.bookingId).isEqualTo(seedOffenderBooking.bookingId)
    assertThat(offenderVisitBalanceAdjustment.adjustDate).isEqualTo(LocalDate.of(2020, 12, 21))
    assertThat(offenderVisitBalanceAdjustment.adjustReasonCode?.code).isEqualTo("VO_ISSUE")
    assertThat(offenderVisitBalanceAdjustment.remainingVisitOrders).isEqualTo(-1)
    assertThat(offenderVisitBalanceAdjustment.previousRemainingVisitOrders).isEqualTo(25)
    assertThat(offenderVisitBalanceAdjustment.commentText).isEqualTo("test comment")
    assertThat(offenderVisitBalanceAdjustment.authorisedStaffId).isEqualTo(123L)
    assertThat(offenderVisitBalanceAdjustment.endorsedStaffId).isEqualTo(123L)
  }
}
