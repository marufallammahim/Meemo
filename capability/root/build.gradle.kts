plugins { id("meemo.android.library") }

dependencies {
    api(project(":capability"))
    implementation(project(":domain"))
    implementation(project(":core:common"))
}
