package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen

@EntityOpen
@Entity
@Table(name = "ASSESSMENTS")
data class Assessment(
  @Id
  @Column(name = "ASSESSMENT_ID")
  val id: Long = 0,

  val assessmentCode: String,

  val assessmentClass: String,
)
