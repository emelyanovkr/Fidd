plugins {
    id("java")
    id("org.openapi.generator") version "7.16.0"
}

val openApiSpecPath = "$rootDir/FiddConnectorRestClient/src/main/resources/openapi/openapi.yaml"

group = "com.fidd"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sourceSets {
    named("main") {
        java.srcDir("src/main/java")
        java.srcDir(layout.buildDirectory.dir("generated/src/main/java"))
    }
    named("test") {
        java.srcDir("src/test/java")
    }
}

dependencies {
    implementation(project(":FiddCore"))

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("io.gsonfire:gson-fire:1.9.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation(project(":FiddConnectorRestServer"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.10"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set(openApiSpecPath)
    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath)
    apiPackage.set("com.fidd.connector.client.api")
    modelPackage.set("com.fidd.connector.client.model")
    invokerPackage.set("com.fidd.connector.client.invoker")

    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)

    configOptions.set(
        mapOf(
            "library" to "okhttp-gson",
            "dateLibrary" to "java8",
            "serializationLibrary" to "gson",
            "hideGenerationTimestamp" to "false"
        )
    )

    globalProperties.set(
        mapOf(
            "apis" to "",
            "models" to "",
            "supportingFiles" to ""
        )
    )
}

tasks.named("compileJava") {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.test {
    useJUnitPlatform()
}
