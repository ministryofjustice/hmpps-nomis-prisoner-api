package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.identifyingmarks

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.BodyPart
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ImageSource
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MarkType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderImageBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderImageIdentifyingMark
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderImageBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderImageIdentifyingMarkRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

class IdentifyingMarksIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var imageSourceRepository: ReferenceCodeRepository<ImageSource>

  @Autowired
  private lateinit var bodyPartRepository: ReferenceCodeRepository<BodyPart>

  @Autowired
  private lateinit var markTypeRepository: ReferenceCodeRepository<MarkType>

  @Autowired
  private lateinit var offenderImageIdentifyingMarkRepository: OffenderImageIdentifyingMarkRepository

  @Autowired
  private lateinit var offenderImageBookingRepository: OffenderImageBookingRepository

  @Autowired
  private lateinit var repository: Repository

  // Temporary tests to check the JPA mappings - to be removed when real tests are available
  @DisplayName("Offender Images JPA tests ")
  @Nested
  inner class TestJpa {
    @Test
    fun `should save and load identifying marks offender images`() {
      lateinit var booking: OffenderBooking
      nomisDataBuilder.build {
        offender {
          booking = booking()
        }
      }
      val imageSourceFile = imageSourceRepository.findByIdOrNull(ImageSource.pk("FILE"))!!
      val bodyPartHead = bodyPartRepository.findByIdOrNull(BodyPart.pk("HEAD"))!!
      val markTypeTattoo = markTypeRepository.findByIdOrNull(MarkType.pk("TAT"))!!
      val image = OffenderImageIdentifyingMark(
        id = 0,
        offenderBooking = booking,
        captureDateTime = LocalDateTime.now(),
        fullSizeImage = byteArrayOf(1, 2, 3),
        thumbnailImage = byteArrayOf(4, 5, 6),
        imageObjectId = 1L,
        active = true,
        imageSource = imageSourceFile,
        orientationType = bodyPartHead,
        imageViewType = markTypeTattoo,
      )
      val imageId = offenderImageIdentifyingMarkRepository.save(image)

      repository.runInTransaction {
        val saved = offenderImageIdentifyingMarkRepository.findByIdOrNull(imageId.id)!!
        with(saved) {
          assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
          assertThat(captureDateTime.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(fullSizeImage).containsExactly(1, 2, 3)
          assertThat(thumbnailImage).containsExactly(4, 5, 6)
          assertThat(imageObjectId).isEqualTo(1L)
          assertThat(active).isTrue()
          assertThat(imageSource.code).isEqualTo("FILE")
          assertThat(orientationType.code).isEqualTo("HEAD")
          assertThat(imageViewType.code).isEqualTo("TAT")
          assertThat(createDatetime.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(createUserId).isEqualTo("SA")
        }
      }
    }

    @Test
    fun `should save and load offender booking offender images`() {
      lateinit var booking: OffenderBooking
      nomisDataBuilder.build {
        offender {
          booking = booking()
        }
      }
      val imageSourceFile = imageSourceRepository.findByIdOrNull(ImageSource.pk("WEB"))!!
      val image = OffenderImageBooking(
        id = 0,
        offenderBooking = booking,
        captureDateTime = LocalDateTime.now(),
        fullSizeImage = byteArrayOf(7, 8, 9),
        thumbnailImage = byteArrayOf(10, 11, 12),
        imageObjectId = 11L,
        active = false,
        imageSource = imageSourceFile,
      )
      val imageId = offenderImageBookingRepository.save(image)

      repository.runInTransaction {
        val saved = offenderImageBookingRepository.findByIdOrNull(imageId.id)!!
        with(saved) {
          assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
          assertThat(captureDateTime.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(fullSizeImage).containsExactly(7, 8, 9)
          assertThat(thumbnailImage).containsExactly(10, 11, 12)
          assertThat(imageObjectId).isEqualTo(11L)
          assertThat(active).isFalse()
          assertThat(imageSource.code).isEqualTo("WEB")
          assertThat(orientationTypeCode).isEqualTo("FRONT")
          assertThat(imageViewTypeCode).isEqualTo("FACE")
          assertThat(createDatetime.toLocalDate()).isEqualTo(LocalDate.now())
          assertThat(createUserId).isEqualTo("SA")
        }
      }
    }
  }
}
