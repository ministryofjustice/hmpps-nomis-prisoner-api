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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncentiveId
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(HmppsAuthenticationHolder::class, AuditorAwareImpl::class, Repository::class)
@WithMockAuthUser
class IncentiveRepositoryTest {
  @Autowired
  lateinit var builderRepository: Repository

  @Autowired
  lateinit var repository: IncentiveRepository

  @Autowired
  lateinit var iepLevelRepository: ReferenceCodeRepository<IEPLevel>

  @Autowired
  lateinit var agencyRepository: AgencyLocationRepository

  @Autowired
  lateinit var entityManager: TestEntityManager

  @Test
  fun saveIncentive() {
    val seedOffenderBooking = builderRepository.save(
      LegacyOffenderBuilder()
        .withBooking(OffenderBookingBuilder()),
    ).latestBooking()

    val incentive = Incentive(
      id = IncentiveId(seedOffenderBooking, 1),
      commentText = "comment text",
      iepDate = LocalDate.parse("2009-12-21"),
      iepTime = LocalDateTime.parse("2009-12-21T13:15"),
      iepLevel = iepLevelRepository.findById(IEPLevel.pk("BAS")).orElseThrow(),
      location = agencyRepository.findById("LEI").orElseThrow(),
      userId = "me",
      auditModuleName = "audit",
    )

    repository.save(incentive)
    entityManager.flush()

    val persistedIncentive = repository.findFirstByIdOffenderBookingOrderByIepDateDescIdSequenceDesc(seedOffenderBooking)
    assertThat(persistedIncentive).isNotNull

    assertThat(persistedIncentive?.commentText).isEqualTo("comment text")
    assertThat(persistedIncentive?.iepDate).isEqualTo(LocalDate.parse("2009-12-21"))
    assertThat(persistedIncentive?.iepTime).isEqualTo(LocalDateTime.parse("2009-12-21T13:15"))
    assertThat(persistedIncentive?.id?.offenderBooking?.bookingId).isEqualTo(seedOffenderBooking.bookingId)
    assertThat(persistedIncentive?.iepLevel?.description).isEqualTo("Basic")
    assertThat(persistedIncentive?.location?.description).isEqualTo("LEEDS")
    assertThat(persistedIncentive?.userId).isEqualTo("me")
    assertThat(persistedIncentive?.auditModuleName).isEqualTo("audit")
  }
}
