package com.space.antivirus.feature.onboarding

/**
 * Static onboarding copy — deliberately its own file, not owned by either
 * OnboardingViewModel or OnboardingScreen, since both need to know how
 * many pages exist (ViewModel for bounds-checking navigation, Screen for
 * rendering). Adding a future page means appending one entry here; no
 * other file needs to change — the "easily extensible for future pages"
 * requirement this sprint asked for.
 *
 * Written to be honest about what this app actually does today, not
 * aspirational: it checks installed applications for permission patterns
 * and identity mismatches (Sprints 014/015) — it does not scan files,
 * messages, photos, or browsing activity, and there is no real-time
 * monitoring yet (that's Phase D). Copy here must not claim otherwise.
 */
data class OnboardingPage(
    val headline: String,
    val body: String,
)

val OnboardingPages: List<OnboardingPage> = listOf(
    OnboardingPage(
        headline = "Welcome to Space Antivirus",
        body = "We help you keep an eye on the apps installed on your device — checking for " +
            "permission patterns and app identities that don't look right, so you can make " +
            "informed decisions about what's on your phone.",
    ),
    OnboardingPage(
        headline = "Your privacy comes first",
        body = "We only look at what's already on your device — installed app details and the " +
            "permissions they request. We don't send your data anywhere, and we don't track " +
            "what you do in other apps.",
    ),
    OnboardingPage(
        headline = "What we check, and what we don't",
        body = "We check your installed apps for suspicious permission combinations and signs " +
            "of impersonating a well-known app. We do not scan your photos, messages, or " +
            "personal files, and we don't monitor your browsing.",
    ),
    OnboardingPage(
        headline = "Trusted Items",
        body = "If a scan ever flags something you know is safe, you can mark it as trusted so " +
            "it won't be flagged again. You can manage your trusted items any time — there's " +
            "nothing to set up right now.",
    ),
)
