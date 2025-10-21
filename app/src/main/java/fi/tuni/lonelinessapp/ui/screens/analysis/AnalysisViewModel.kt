package fi.tuni.lonelinessapp.ui.screens.analysis

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sqrt

class AnalysisViewModel : ViewModel() {

    // Näytön (UI) tila (laajennataan kun data kytketään).
    data class UiState(

        // näytetäänkö paikkamerkit.
        val isLoading: Boolean = false,

        // virheviesti, jos haku epäonnistuu.
        val error: String? = null
    )

    // 'ui' on muokattava tila, jota ViewModel voi päivittää.
    private val _ui = MutableStateFlow(UiState())

    // 'ui' on vain luettava versio, jota käyttöliittymä voi seurata
    val ui: StateFlow<UiState> = _ui

    // Datan rakenteet.

    // Yhden päivän tiedot (pvm, kyselyn tulos, puhelimen käyttö, askeleet).
    data class DaySample(

        // Päivämäärä.
        val date: LocalDate,

        // Kyselyn tulos (UCLA 0–9).
        val loneliness: Float,

        // Puhelimen käyttö yöllä (minuutteina).
        // MAHDOLLINEN MUUTOS? Riippuen missä muodossa tulokset tulevat.
        val nightMinutes: Float,

        // Puhelimen käyttö päivällä (minuutteina).
        // MAHDOLLINEN MUUTOS? Riippuen missä muodossa tulokset tulevat.
        val dayMinutes: Float,

        // Askeleet.
        val steps: Float
    )

    // Yhden pisteen tiedot viivakaavioon.
    data class LinePoint(val xLabel: String, val y: Float)

    // Yhden pisteen tiedot pylväskaavioon.
    data class BarPoint (val xLabel: String, val y: Float)

    // Yhden viipaleen tiedot piirakkakaavioon.
    data class PieSlice (val label: String, val value: Float)

    // Testidata (korvataan myöhemmin oikealla datalla).

    // Luo 7 päivän esimerkkidatan.
    fun loadCurrentWeek(): List<DaySample> {

        // Aloituspäivä 6 päivää sitten
        val start = java.time.LocalDate.now().minusDays(6)

        // Esimerkkidata.
        val lon = listOf(2.4f, 1.8f, 2.1f, 1.5f, 1.2f, 1.0f, 1.4f)
        val night = listOf(38f, 29f, 47f, 22f, 35f, 54f, 31f)
        val day   = listOf(165f,150f,180f,140f,172f,200f,200f)
        val steps = listOf(8000f,9000f,7500f,10000f,8200f,20000f,11000f)


        // Palautetaan lista, jossa jokaiselle päivälle omat tiedot.
        return (0..6).map { i ->
            DaySample(
                date = start.plusDays(i.toLong()),
                loneliness = lon[i],
                nightMinutes = night[i],
                dayMinutes = day[i],
                steps = steps[i]
            )
        }
    }
    // Tehdään uunnokset kaavioita varten.

    // Muuntaa päivän datan viivakaavioon sopivaksi (päivän nimi + arvo).
    fun lonelinessLine(data: List<DaySample>): List<LinePoint> =
        data.map { d -> LinePoint(d.date.dayOfWeek.name.take(3), d.loneliness) }

    // Muuntaa yöminuutit tunneiksi pylväskaaviota varten.
    fun nightUsageBarsHours(data: List<DaySample>): List<BarPoint> =
        data.map { d -> BarPoint(d.date.dayOfWeek.name.take(3), minutesToHours(d.nightMinutes)) }

    // Muuntaa päiväminuutit tunneiksi pylväskaaviota varten.
    fun dayUsageBarsHours(data: List<DaySample>): List<BarPoint> =
        data.map { d -> BarPoint(d.date.dayOfWeek.name.take(3), minutesToHours(d.dayMinutes)) }

    // Luo askeleiden datan sellaisenaan (ei muutosta yksiköissä).
    fun stepsBars(data: List<DaySample>): List<BarPoint> =
        data.map { d -> BarPoint(d.date.dayOfWeek.name.take(3), d.steps) }

    // Luo viestintäsovellusten tunnit piirakkakaaviolle.
    fun communicationPieHours(): List<PieSlice> = listOf(
        PieSlice("WhatsApp", 2.3f),
        PieSlice("Messages", 1.7f),
        PieSlice("Calls",    0.9f),
        PieSlice("Signal",   0.6f),
        PieSlice("Telegram",  0.5f)
    )

    // Apufunktiot.

    // Muuntaa minuutit tunneiksi.
    private fun minutesToHours(mins: Float): Float = mins / 60f

    // Pyöristää luvun yhteen desimaaliin.
    private fun round1(v: Float) = (round(v * 10f) / 10f)

    // Pyöristää lähimpään kokonaislukuun.
    private fun round0(v: Float) = round(v)


}