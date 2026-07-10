package com.cartablet.utils

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

object TotpHelper {
    fun verifyCode(secret: String, code: String): Boolean {
        if (code.length != 6) return false
        val secretBytes = try { decodeBase32(secret) } catch (_: Exception) { return false }
        val currentTime = System.currentTimeMillis() / 1000 / 30
        
        // Check current, previous, and next windows to account for slight clock drift
        for (i in -1..1) {
            if (generateTOTP(secretBytes, currentTime + i) == code) return true
        }
        return false
    }

    private fun generateTOTP(secret: ByteArray, interval: Long): String {
        val data = ByteBuffer.allocate(8).putLong(interval).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secret, "HmacSHA1"))
        val hash = mac.doFinal(data)
        
        val offset = hash[hash.size - 1].toInt() and 0xf
        val truncatedHash = ((hash[offset].toInt() and 0x7f shl 24) or
                            (hash[offset + 1].toInt() and 0xff shl 16) or
                            (hash[offset + 2].toInt() and 0xff shl 8) or
                            (hash[offset + 3].toInt() and 0xff))
        
        val otp = truncatedHash % 10.0.pow(6.0).toInt()
        return otp.toString().padStart(6, '0')
    }

    private fun decodeBase32(base32: String): ByteArray {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleanInput = base32.uppercase().replace(" ", "")
        var bits = 0
        var value = 0
        val out = mutableListOf<Byte>()
        
        for (char in cleanInput) {
            val index = base32Chars.indexOf(char)
            if (index == -1) continue
            value = (value shl 5) or index
            bits += 5
            if (bits >= 8) {
                out.add(((value shr (bits - 8)) and 0xFF).toByte())
                bits -= 8
            }
        }
        return out.toByteArray()
    }
}
