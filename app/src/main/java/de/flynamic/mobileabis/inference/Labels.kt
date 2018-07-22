package de.flynamic.mobileabis.inference

import android.app.Activity
import android.util.Log
import java.io.File
import java.io.InputStream

class Labels(activity: Activity) {
    public var labels: MutableList<String>

    init {
        val inputStream: InputStream = activity.assets.open("labels.txt")
        val lineList = mutableListOf<String>()
        this.labels = mutableListOf()
        inputStream.bufferedReader().useLines { lines -> lines.forEach { lineList.add(it) } }
        lineList.forEach {
            val index = it.substringBefore(' ').toInt()
            val species = it.substringAfter(' ')
            this.labels.add(index, species)
        }
    }
}