package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.type.YesNoConverter
import java.io.Serializable

@Embeddable
data class SentenceCalculationTypeId(
  @Column(name = "SENTENCE_CALC_TYPE", nullable = false)
  val calculationType: String,

  @Column(name = "SENTENCE_CATEGORY", nullable = false)
  val category: String,
) : Serializable

// Warning: on mandatory fields mapped - just enough to create test entities
@Entity
@Table(name = "SENTENCE_CALC_TYPES")
class SentenceCalculationType(
  @EmbeddedId
  val id: SentenceCalculationTypeId,
  @Column(name = "DESCRIPTION")
  val description: String,
  @Column(name = "SENTENCE_TYPE")
  val sentenceType: String,
  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = false,
)
