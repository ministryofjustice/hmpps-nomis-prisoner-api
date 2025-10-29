package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class AgencyLocationDslMarker

@NomisDataDslMarker
interface AgencyLocationDsl {
  @AgencyLocationAddressDslMarker
  fun address(
    type: String? = "BUS",
    premise: String? = "2",
    street: String? = "Gloucester Terrace",
    locality: String? = "Stanningley Road",
    flat: String? = null,
    postcode: String? = "LS12 2TJ",
    city: String? = "29059",
    county: String? = "W.YORKSHIRE",
    country: String? = "ENG",
    validatedPAF: Boolean = false,
    noFixedAddress: Boolean? = null,
    primaryAddress: Boolean = true,
    mailAddress: Boolean = false,
    comment: String? = null,
    startDate: String? = null,
    endDate: String? = null,
    isServices: Boolean = false,
    businessHours: String? = null,
    contactPersonName: String? = null,
    whenCreated: LocalDateTime? = null,
    whoCreated: String? = null,
    dsl: AgencyLocationAddressDsl.() -> Unit = {},
  ): AgencyLocationAddress
}

@Component
class AgencyLocationBuilderRepository(
  private val agencyLocationRepository: AgencyLocationRepository,
  private val agencyTypeRepository: ReferenceCodeRepository<AgencyLocationType>,
) {
  fun save(agencyLocation: AgencyLocation): AgencyLocation = agencyLocationRepository.saveAndFlush(agencyLocation)
  fun agencyTypeOf(code: String): AgencyLocationType = agencyTypeRepository.findByIdOrNull(AgencyLocationType.pk(code))!!
}

@Component
class AgencyLocationBuilderFactory(
  private val repository: AgencyLocationBuilderRepository,
  private val agencyAddressBuilderFactory: AgencyLocationAddressBuilderFactory,
) {
  fun builder(): AgencyLocationBuilder = AgencyLocationBuilder(repository, agencyAddressBuilderFactory)
}

class AgencyLocationBuilder(
  private val repository: AgencyLocationBuilderRepository,
  private val agencyAddressBuilderFactory: AgencyLocationAddressBuilderFactory,
) : AgencyLocationDsl {
  private lateinit var agencyLocation: AgencyLocation

  fun build(
    id: String,
    description: String,
    type: String,
    active: Boolean,
  ): AgencyLocation = AgencyLocation(
    id = id,
    description = description,
    type = repository.agencyTypeOf(type),
    active = active,
  )
    .let { repository.save(it) }
    .also { agencyLocation = it }

  override fun address(
    type: String?,
    premise: String?,
    street: String?,
    locality: String?,
    flat: String?,
    postcode: String?,
    city: String?,
    county: String?,
    country: String?,
    validatedPAF: Boolean,
    noFixedAddress: Boolean?,
    primaryAddress: Boolean,
    mailAddress: Boolean,
    comment: String?,
    startDate: String?,
    endDate: String?,
    isServices: Boolean,
    businessHours: String?,
    contactPersonName: String?,
    whenCreated: LocalDateTime?,
    whoCreated: String?,
    dsl: AgencyLocationAddressDsl.() -> Unit,
  ): AgencyLocationAddress = agencyAddressBuilderFactory.builder().let { builder ->
    builder.build(
      agencyLocation = agencyLocation,
      type = type,
      premise = premise,
      street = street,
      locality = locality,
      flat = flat,
      postcode = postcode,
      city = city,
      county = county,
      country = country,
      validatedPAF = validatedPAF,
      noFixedAddress = noFixedAddress,
      primaryAddress = primaryAddress,
      mailAddress = mailAddress,
      comment = comment,
      startDate = startDate?.let { LocalDate.parse(it) },
      endDate = endDate?.let { LocalDate.parse(it) },
      isServices = isServices,
      businessHours = businessHours,
      contactPersonName = contactPersonName,
    )
      .also { agencyLocation.addresses += it }
      .also { builder.apply(dsl) }
  }
}
