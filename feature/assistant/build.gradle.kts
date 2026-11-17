plugins {
    id("meemo.android.library")
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":nlu"))
    implementation(project(":voice"))
    implementation(project(":capability"))
    implementation(project(":core:ui"))
    implementation(project(":core:model"))
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
}
