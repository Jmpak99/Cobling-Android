// 색상 관련 확장 함수
package com.cobling.app.view.quest

import androidx.compose.ui.graphics.Color

fun HexColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")

    val colorLong = when (cleanHex.length) {
        6 -> ("FF$cleanHex").toLong(16)   // RGB -> ARGB
        8 -> cleanHex.toLong(16)          // 이미 ARGB
        else -> throw IllegalArgumentException("잘못된 hex 색상값입니다: $hex")
    }

    return Color(colorLong)
}