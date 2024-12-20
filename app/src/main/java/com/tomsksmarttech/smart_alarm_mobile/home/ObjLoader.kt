package com.tomsksmarttech.smart_alarm_mobile.home

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Vector


class ObjLoader(context: Context, file: String?) {
    val numFaces: Int

    val normals: FloatArray
    val textureCoordinates: FloatArray
    val positions: FloatArray

    init {
        val vertices = Vector<Float>()
        val normals = Vector<Float>()
        val textures = Vector<Float>()
        val faces = Vector<String>()

        var reader: BufferedReader? = null
        try {
            val `in` = InputStreamReader(
                context.assets.open(
                    file!!
                )
            )
            reader = BufferedReader(`in`)

            // read file until EOF
            var line: String
            while ((reader.readLine().also { line = it }) != null) {
                val parts = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                when (parts[0]) {
                    "v" -> {
                        // vertices
                        vertices.add(parts[1].toFloat())
                        vertices.add(parts[2].toFloat())
                        vertices.add(parts[3].toFloat())
                    }

                    "vt" -> {
                        // textures
                        textures.add(parts[1].toFloat())
                        textures.add(parts[2].toFloat())
                    }

                    "vn" -> {
                        // normals
                        normals.add(parts[1].toFloat())
                        normals.add(parts[2].toFloat())
                        normals.add(parts[3].toFloat())
                    }

                    "f" -> {
                        // faces: vertex/texture/normal
                        faces.add(parts[1])
                        faces.add(parts[2])
                        faces.add(parts[3])
                    }
                }
            }
        } catch (e: IOException) {
            // cannot load or read file
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    //log the exception
                }
            }
        }

        numFaces = faces.size
        this.normals = FloatArray(numFaces * 3)
        textureCoordinates = FloatArray(numFaces * 2)
        positions = FloatArray(numFaces * 3)
        var positionIndex = 0
        var normalIndex = 0
        var textureIndex = 0
        for (face in faces) {
            val parts = face.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            var index = 3 * (parts[0].toShort() - 1)
            positions[positionIndex++] = vertices[index++]
            positions[positionIndex++] = vertices[index++]
            positions[positionIndex++] = vertices[index]

            index = 2 * (parts[1].toShort() - 1)
            textureCoordinates[normalIndex++] = textures[index++]
            // NOTE: Bitmap gets y-inverted
            textureCoordinates[normalIndex++] = 1 - textures[index]

            index = 3 * (parts[2].toShort() - 1)
            this.normals[textureIndex++] = normals[index++]
            this.normals[textureIndex++] = normals[index++]
            this.normals[textureIndex++] = normals[index]
        }
    }
}