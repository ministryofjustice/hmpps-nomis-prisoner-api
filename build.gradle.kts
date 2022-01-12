plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.0.1"
  kotlin("plugin.spring") version "1.6.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.flywaydb:flyway-core:8.4.1")
  implementation("com.vladmihalcea:hibernate-types-52:2.14.0")

  implementation("org.springdoc:springdoc-openapi-ui:1.6.4")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.4")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.4")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")

  implementation("com.zaxxer:HikariCP:5.0.1")
  runtimeOnly("com.h2database:h2:2.0.206")
  runtimeOnly("com.oracle.database.jdbc:ojdbc10:19.13.0.0.1")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("org.mockito:mockito-inline:4.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.0.29")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
}
