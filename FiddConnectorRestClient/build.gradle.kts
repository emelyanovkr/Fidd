plugins {
    id("java")
}
val openApiSpecPath = "$rootDir/FiddConnectorRestClient/src/main/resources/openapi/openapi.yaml"

group = "com.fidd"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
