package com.tricare.manuals.data.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Date

object NtpClient {
    fun getTime(): Date {
        return try {
            val socket = DatagramSocket()
            socket.soTimeout = 3000
            val address = InetAddress.getByName("time.google.com")
            val buf = ByteArray(48).also { it[0] = 0x1B.toByte() }
            socket.send(DatagramPacket(buf, buf.size, address, 123))
            val response = DatagramPacket(ByteArray(48), 48)
            socket.receive(response)
            socket.close()
            // Extract transmit timestamp from bytes 40-47
            val data = response.data
            var seconds = 0L
            for (i in 40..43) seconds = seconds shl 8 or (data[i].toLong() and 0xFF)
            // NTP epoch is Jan 1 1900; Unix epoch is Jan 1 1970 (difference = 2208988800)
            Date((seconds - 2208988800L) * 1000)
        } catch (e: Exception) {
            Date() // fall back to device clock
        }
    }
}
