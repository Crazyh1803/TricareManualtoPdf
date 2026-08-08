package com.tricare.manuals.data.network

/** Official DHA source for TRICARE manuals — the app scrapes this domain and must
 *  always be able to point the user back to it (Play Store Misleading Claims policy). */
object TricareUrls {
    const val OFFICIAL_SITE = "https://manuals.health.mil"
    const val TOC_BASE = "$OFFICIAL_SITE/pages/ManualToc.aspx?Manual="
}
