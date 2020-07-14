package com.rtuitlab.geohelper

import org.junit.Test

import org.junit.Assert.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class BearingTest {
	@Test
	fun bearing_isCorrect() {
		val mirea = placesListTest[0]
		val mine = Place(
			55.928823, 39.456725, "Home"
		)
		assertEquals(
			256.89,
			bearing(mine, mirea),
			.000001
		)
	}

	@Test
	fun dekart_isCorrect() {
		val distant = 3.0

		val mirea = placesListTest[0]
		val mine = Place(
			55.928823, 39.456725, "Home"
		)
		val degree = bearing(mine, mirea)

		val y = 0F
		val x = (distant * cos(PI * degree / 180)).toFloat()
		val z = (-distant * sin(PI * degree / 180)).toFloat()

		assertEquals(
			"X: $x; Y: $y; Z: $z",
			true, false
		)
	}
}

val placesListTest: List<Place> = listOf(
	Place(55.670002, 37.480212, "РТУ МИРЭА"),
	Place(56.126895, 40.397134, "Золотые ворота"),
	Place(59.939817, 30.314448, "Эрмитаж"),
	Place(58.939817, 35.314448, "Эрмитаж1"),
	Place(57.939817, 34.314448, "Эрмитаж2"),
	Place(-47.342052, -68.559948, "Эрмитаж3"),
	Place(20.206243, -75.167841, "Эрмитаж4"),
	Place(48.900373, -73.653973, "Эрмитаж5")
)

fun bearing(from: Place, to: Place): Double {
	val lat1 = from.latitude / 180 * PI
	val lng1 = from.longitude / 180 * PI
	val lat2 = to.latitude / 180 * PI
	val lng2 = to.longitude / 180 * PI

	val x = cos(lat2) * sin(lng2 - lng1)
	val y = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lng2 - lng1)

	return ((atan2(x, y) * 180 / PI) + 360) % 360
}

