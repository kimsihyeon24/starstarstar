package com.example.starfinder

/**
 * 별자리 선. 각 선은 Stars.kt에 있는 별의 영어 이름(en) 두 개를 잇는다.
 * asterism = true 는 공식 별자리가 아닌 '모양'(대삼각형 등) → 점선으로 표시
 */
data class Constellation(
    val ko: String,
    val lines: List<Pair<String, String>>,
    val asterism: Boolean = false
)

val CONSTELLATIONS = listOf(
    Constellation(
        "큰곰자리 (북두칠성)", listOf(
            "Dubhe" to "Merak", "Merak" to "Phecda", "Phecda" to "Megrez",
            "Megrez" to "Dubhe", "Megrez" to "Alioth", "Alioth" to "Mizar",
            "Mizar" to "Alkaid"
        )
    ),
    Constellation(
        "카시오페이아자리", listOf(
            "Caph" to "Schedar", "Schedar" to "Navi",
            "Navi" to "Ruchbah", "Ruchbah" to "Segin"
        )
    ),
    Constellation(
        "오리온자리", listOf(
            "Meissa" to "Betelgeuse", "Meissa" to "Bellatrix",
            "Betelgeuse" to "Alnitak", "Bellatrix" to "Mintaka",
            "Mintaka" to "Alnilam", "Alnilam" to "Alnitak",
            "Alnitak" to "Saiph", "Mintaka" to "Rigel"

        )
    ),
    Constellation(
        "백조자리", listOf(
            "Deneb" to "Sadr", "Sadr" to "Albireo",
            "Gienah" to "Sadr", "Sadr" to "Fawaris"
        )
    ),
    Constellation(
        "독수리자리", listOf("Tarazed" to "Altair", "Altair" to "Alshain")
    ),
    Constellation(
        "페가수스자리", listOf(
            "Alpheratz" to "Scheat", "Scheat" to "Markab",
            "Markab" to "Algenib", "Algenib" to "Alpheratz",
            "Markab" to "Enif"
        )
    ),
    Constellation(
        "안드로메다자리", listOf("Alpheratz" to "Mirach", "Mirach" to "Almach")
    ),
    Constellation(
        "사자자리", listOf(
            "Regulus" to "Algieba", "Algieba" to "Zosma", "Zosma" to "Denebola",
            "Denebola" to "Chertan", "Chertan" to "Regulus"
        )
    ),
    Constellation(
        "큰개자리", listOf("Mirzam" to "Sirius", "Sirius" to "Wezen", "Wezen" to "Adhara")
    ),
    Constellation(
        "쌍둥이자리", listOf("Castor" to "Pollux")

    ),
    Constellation(
        "여름철 대삼각형",
        listOf("Vega" to "Deneb", "Deneb" to "Altair", "Altair" to "Vega"),
        asterism = true
    ),
    Constellation(
        "겨울철 대삼각형",
        listOf("Betelgeuse" to "Sirius", "Sirius" to "Procyon", "Procyon" to "Betelgeuse"),
        asterism = true
    ),
)