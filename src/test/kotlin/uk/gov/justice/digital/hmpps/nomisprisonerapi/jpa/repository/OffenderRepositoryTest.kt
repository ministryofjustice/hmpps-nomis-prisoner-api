package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(HmppsAuthenticationHolder::class, AuditorAwareImpl::class, Repository::class)
@WithMockAuthUser
class OffenderRepositoryTest {

  @Autowired
  lateinit var builderRepository: Repository

  @Autowired
  lateinit var repository: OffenderRepository

  @Test
  fun findAllIds() {
    builderRepository.save(OffenderDataBuilder().withBooking(OffenderBookingDataBuilder()))
    builderRepository.save(OffenderDataBuilder().withBooking(OffenderBookingDataBuilder()))
    builderRepository.save(OffenderDataBuilder().withBooking(OffenderBookingDataBuilder()))

    assertThat(repository.findAllIds(Pageable.ofSize(10)).numberOfElements).isEqualTo(3)
    assertThat(repository.findAllIds(PageRequest.of(1, 2)).numberOfElements).isEqualTo(1)
  }

  @Test
  fun findAllIdsFromId() {
    val seedOffender1: Offender = builderRepository.save(
      OffenderDataBuilder().withBooking(OffenderBookingDataBuilder()),
    )

    val seedOffender2: Offender = builderRepository.save(
      OffenderDataBuilder().withBooking(OffenderBookingDataBuilder()),
    )

    val seedOffender3: Offender = builderRepository.save(
      OffenderDataBuilder().withBooking(OffenderBookingDataBuilder()),
    )

    assertThat(repository.findAllIdsFromId(0, 10).size).isEqualTo(3)
    assertThat(repository.findAllIdsFromId(seedOffender1.id, 10).size).isEqualTo(2)
    assertThat(repository.findAllIdsFromId(seedOffender2.id, 10).size).isEqualTo(1)
    assertThat(repository.findAllIdsFromId(seedOffender3.id, 10).size).isEqualTo(0)
    assertThat(repository.findAllIdsFromId(0, 2).size).isEqualTo(2)
  }
}
