package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens semánticos del sistema visual institucional SASE Light.
 *
 * Familias:
 * - Institucionales → navegación, encabezados, botones primarios
 * - Grado → credenciales, chips de grado, filtros
 * - Funcionales → estados (éxito, advertencia, error, información)
 * - Superficies → fondos, tarjetas, contenedores
 * - Texto → jerarquía de legibilidad
 * - Bordes → contenedores, campos, separadores
 *
 * CONTRASTE (sobre Background #F8FCFF / Surface #FFFFFF):
 *   TextPrimary (#0F172A)    → ratio ~15.1:1 — PASS AAA
 *   TextSecondary (#475569)  → ratio ~5.5:1  — PASS AA (texto ≥14pt) / AAA (≥18pt)
 *   TextDisabled  (#94A3B8)  → ratio ~2.5:1  — solo para no interactivo
 *   Placeholder   (#64748B)  → ratio ~4.6:1  — PASS AA (texto ≥14pt)
 */
object SaseColors {

    // ── Institucionales ────────────────────────────────────────────────
    val InstitutionalBlue = Color(0xFF2563EB)
    val InstitutionalBlueDark = Color(0xFF1E40AF)
    val InstitutionalBlueLight = Color(0xFFDBEAFE)
    val InstitutionalNavy = Color(0xFF062B3A)
    val InstitutionalNavyDark = Color(0xFF021C28)

    // ── Grado (credenciales, indicadores) ──────────────────────────────
    val GradeOneGreen = Color(0xFF16A34A)
    val GradeTwoGold = Color(0xFFCA8A04)
    val GradeThreeRed = Color(0xFFB91C1C)

    // ── Funcionales ─────────────────────────────────────────────────────
    val Success = Color(0xFF16A34A)
    val SuccessBackground = Color(0xFFF0FDF4)
    val Warning = Color(0xFFD97706)
    val WarningBackground = Color(0xFFFFFBEB)
    val Error = Color(0xFFDC2626)
    val ErrorBackground = Color(0xFFFEF2F2)
    val Information = Color(0xFF2563EB)
    val InformationBackground = Color(0xFFEFF6FF)
    val Neutral = Color(0xFF64748B)
    val NeutralBackground = Color(0xFFF8FAFC)

    // ── Superficies ─────────────────────────────────────────────────────
    val Background = Color(0xFFF8FCFF)
    val Surface = Color.White
    val SurfaceVariant = Color(0xFFF1F5F9)
    val SurfaceElevated = Color.White

    // ── Texto ───────────────────────────────────────────────────────────
    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF475569)
    val TextDisabled = Color(0xFF94A3B8)
    val Placeholder = Color(0xFF64748B)
    val TextOnPrimary = Color.White
    val TextOnDark = Color.White

    // ── Bordes ──────────────────────────────────────────────────────────
    val Border = Color(0xFFCBD5E1)
    val BorderStrong = Color(0xFF94A3B8)
    val BorderFocus = Color(0xFF2563EB)
    val BorderDisabled = Color(0xFFE2E8F0)
}
