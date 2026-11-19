plugins {
    id("meemo.android.library")
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:datastore"))
    implementation(project(":core:ui"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
}
