package org.ligand.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ligand.shared.generated.resources.Res
import ligand.shared.generated.resources.ibmplexsansarabic_bold
import ligand.shared.generated.resources.ibmplexsansarabic_medium
import ligand.shared.generated.resources.ibmplexsansarabic_regular
import ligand.shared.generated.resources.ibmplexsansarabic_semibold
import org.jetbrains.compose.resources.Font


@Composable
fun IBMPlexSansArabicFamily() = FontFamily(
    Font(
        Res.font.ibmplexsansarabic_regular,
        FontWeight.Normal,
    ),
    Font(
        Res.font.ibmplexsansarabic_medium,
        FontWeight.Medium
    ),
    Font(
        Res.font.ibmplexsansarabic_semibold,
        FontWeight.SemiBold,
    ),
    Font(
        Res.font.ibmplexsansarabic_bold,
        FontWeight.Bold,
    ),
)

@Composable
fun appTypography() = Typography().run {
    val fontFamily = IBMPlexSansArabicFamily()

    copy(
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
    )
}

