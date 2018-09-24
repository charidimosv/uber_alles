package com.team.eddie.uber_alles.utils.map

import android.graphics.Color
import android.os.AsyncTask
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class MapRouteHelper {

    var map: GoogleMap? = null
    private val polylines = ArrayList<Polyline>()

    fun drawRoute(latLngList: List<LatLng>) {
        if (latLngList.size < 2) return
        for (i in 0 until latLngList.size - 1)
            drawRoute(latLngList.get(i), latLngList.get(i + 1))
    }

    fun drawRoute(origin: LatLng, dest: LatLng) {
        // Getting URL to the Google Directions API
        val url = getDirectionsUrl(origin, dest)

        val downloadTask = DownloadTask()

        // Start downloading json data from Google Directions API
        downloadTask.execute(url)
    }

    fun cleanRoute() {
        for (line in polylines)
            line.remove()
        polylines.clear()
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {

        val routeOrigStr = "origin=" + origin.latitude + "," + origin.longitude
        val routeDestStr = "destination=" + dest.latitude + "," + dest.longitude

        // Sensor enabled
        val sensor = "sensor=false"
        val mode = "mode=driving"

        // Building the parameters to the web service
        val parameters = "$routeOrigStr&$routeDestStr&$sensor&$mode"

        // Output format
        val output = "json"

        // Building the url to the web service
        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters"
    }

    private fun downloadUrl(strUrl: String): String {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(strUrl)

            urlConnection = url.openConnection() as HttpURLConnection

            urlConnection.connect()

            iStream = urlConnection.inputStream

            val br = BufferedReader(InputStreamReader(iStream!!))

            val sb = StringBuffer()

            var line = br.readLine()
            while (line != null) {
                sb.append(line)
                line = br.readLine()
            }

            data = sb.toString()

            br.close()

        } catch (e: Exception) {
            Log.d("Exception", e.toString())
        } finally {
            iStream!!.close()
            urlConnection!!.disconnect()
        }
        return data
    }

    private inner class DownloadTask : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg url: String): String {

            var data = ""

            try {
                data = downloadUrl(url[0])
            } catch (e: Exception) {
                Log.d("Background Task", e.toString())
            }

            return data
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            val parserTask = ParserTask()
            parserTask.execute(result)
        }
    }

    private inner class ParserTask : AsyncTask<String, Int, List<List<HashMap<String, String>>>>() {

        // Parsing the data in non-ui thread
        override fun doInBackground(vararg jsonData: String): List<List<HashMap<String, String>>>? {

            val jObject: JSONObject
            var routes: List<List<HashMap<String, String>>>? = null

            try {
                jObject = JSONObject(jsonData[0])
                val parser = DirectionsJSONParser()

                routes = parser.parse(jObject)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return routes
        }

        override fun onPostExecute(result: List<List<HashMap<String, String>>>) {
            var points: ArrayList<LatLng>? = null
            var lineOptions: PolylineOptions? = null

            for (i in result.indices) {
                points = ArrayList()
                lineOptions = PolylineOptions()

                val path = result[i]

                for (j in path.indices) {
                    val point = path[j]

                    val lat = java.lang.Double.parseDouble(point["lat"]!!)
                    val lng = java.lang.Double.parseDouble(point["lng"]!!)
                    val position = LatLng(lat, lng)

                    points.add(position)
                }

                lineOptions.addAll(points)
                lineOptions.width(12f)
                lineOptions.color(Color.BLUE)
                lineOptions.geodesic(true)
            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null)
                polylines.add(map!!.addPolyline(lineOptions))
        }
    }
}
