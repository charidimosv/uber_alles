package com.team.eddie.uber_alles.view

class HistoryObject(var rideId: String?, var time: String?) {
    companion object {

        private var lastContactId = 0

        fun createHistoryList(numHistory: Int): ArrayList<HistoryObject> {
            val contacts = ArrayList<HistoryObject>()

            for (i in 1..numHistory) contacts.add(HistoryObject("Person " + ++lastContactId, "time" + ++lastContactId))

            return contacts
        }
    }
}
