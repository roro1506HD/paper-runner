plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

group = "ovh.roro.libraries"
version = "1.2.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.konghq:unirest-java-core:4.8.1")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.google.guava:guava:33.6.0-jre")
}

tasks.compileJava.configure {
    options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    options.release.set(17)
}

tasks.javadoc.configure {
    options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
}

tasks.processResources.configure {
    filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
}

tasks.shadowJar.configure {
    archiveClassifier.set("")
    archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")

    dependencies {
        exclude(dependency("org.jetbrains:annotations"))
    }
}

tasks.jar.configure {
    manifest {
        attributes["Main-Class"] = "ovh.roro.libraries.paperrunner.PaperRunner"
    }
}
