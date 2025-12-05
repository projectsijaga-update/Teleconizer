pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Teleconizer"
include(":app")

// --- FIX KHUSUS AARCH64 (KALI LINUX/ARM) ---
// Kode ini aman untuk GitHub Actions (x86) karena hanya akan aktif
// jika Gradle meminta aapt2. GitHub akan pakai versi defaultnya,
// sedangkan Kali Linux Anda akan dipaksa pakai versi linux-aarch64.
gradle.lifecycle.beforeProject {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.android.tools.build" && requested.name == "aapt2") {
                // Deteksi Arsitektur Sistem Operasi
                val osArch = System.getProperty("os.arch")
                if (osArch == "aarch64" || osArch.startsWith("arm")) {
                    println(">>> SYSTEM ARM64 DETECTED: FORCE AAPT2 LINUX-AARCH64")
                    useTarget("com.android.tools.build:aapt2:${requested.version}:linux-aarch64")
                }
            }
        }
    }
}
