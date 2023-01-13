package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "SENTENCE_ADJUSTMENTS")
class SentenceAdjustment(
  @Id
  @Column(name = "SENTENCE_ADJUST_CODE", nullable = false)
  val id: String,
  @Column(name = "DESCRIPTION", nullable = false)
  val description: String,
)
