package com.example.comp3606_a1_student.network

import android.util.Log
import com.google.gson.Gson
import com.example.comp3606_a1_student.models.ContentModel
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket
import kotlin.concurrent.thread

import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.SecretKey
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }

fun getFirstNChars(str: String, n:Int) = str.substring(0,n)

class Client (private val networkMessageInterface: NetworkMessageInterface){
    private lateinit var clientSocket: Socket
    private lateinit var reader: BufferedReader
    private lateinit var writer: BufferedWriter
    var ip:String = ""
    var studentID = ""

    private fun isRandomNumber(message: String): Boolean {
        return message.toIntOrNull()?.let { it in 0..100} ?: false
    }

    private fun hashStrSha256(str: String): String{
        val algorithm = "SHA-256"
        val hashedString = MessageDigest.getInstance(algorithm).digest(str.toByteArray(UTF_8))
        return hashedString.toHex()
    }

    private fun generateAESKey(seed: String): SecretKeySpec {
        val first32Chars = getFirstNChars(seed,32)
        val secretKey = SecretKeySpec(first32Chars.toByteArray(), "AES")
        return secretKey
    }

    private fun generateIV(seed: String): IvParameterSpec {
        val first16Chars = getFirstNChars(seed, 16)
        return IvParameterSpec(first16Chars.toByteArray())
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun encryptMessage(plaintext: String, aesKey:SecretKey, aesIv: IvParameterSpec):String{
        val plainTextByteArr = plaintext.toByteArray()

        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, aesIv)

        val encrypt = cipher.doFinal(plainTextByteArr)
        return Base64.Default.encode(encrypt)
    }

    init {
        thread {
            try {
                clientSocket = Socket("192.168.49.1", 9999) //clientSocket = Socket("192.168.49.1", Server.PORT)
                reader = clientSocket.inputStream.bufferedReader()
                writer = clientSocket.outputStream.bufferedWriter()
                ip = clientSocket.inetAddress.hostAddress!!

                while (true) {
                    try {
                        val serverResponse = reader.readLine()
                        if (serverResponse != null) {
                            val serverContent = Gson().fromJson(serverResponse, ContentModel::class.java)

                            if (serverContent.senderIp == "192.168.49.1" && isRandomNumber(serverContent.message)) {
                                Log.d("CLIENT", "Received R from server: ${serverContent.message}")

                                val strongStudentID = hashStrSha256(studentID)
                                val aesKey = generateAESKey(strongStudentID)
                                val aesIv = generateIV(strongStudentID)

                                val R = {serverContent.message}.toString()
                                val cypherText = encryptMessage(R, aesKey, aesIv)

                                val hashedID = ContentModel(cypherText, ip)
                                sendMessage(hashedID)
                            }

                            networkMessageInterface.onContent(serverContent)
                        }
                    } catch (e: Exception) {
                        Log.e("CLIENT", "An error has occurred in the client")
                        e.printStackTrace()
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("CLIENT", "Failed to connect to the server")
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(content: ContentModel){
        thread {
            if (!clientSocket.isConnected){
                throw Exception("We aren't currently connected to the server!")
            }
            val contentAsStr:String = Gson().toJson(content)
            writer.write("$contentAsStr\n")
            writer.flush()
        }

    }

    fun close(){
        clientSocket.close()
    }
}