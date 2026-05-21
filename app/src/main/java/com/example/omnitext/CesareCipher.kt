package com.example.omnitext

object CesareCipher {
    private const val CHIAVE = 3 // Puoi cambiarla con qualsiasi numero da 1 a 25

    // Funzione per criptare il testo prima di mandarlo a Firestore
    fun cifra(testo: String): String {
        return testo.map { carattere ->
            when {
                carattere.isUpperCase() -> {
                    ((carattere.code - 'A'.code + CHIAVE) % 26 + 'A'.code).toChar()
                }
                carattere.isLowerCase() -> {
                    ((carattere.code - 'a'.code + CHIAVE) % 26 + 'a'.code).toChar()
                }
                else -> carattere // Lascia invariati spazi, numeri e punteggiatura
            }
        }.joinToString("")
    }

    // Funzione per decifrare il testo quando lo scarichi da Firestore
    fun decifra(testoCifrato: String): String {
        return testoCifrato.map { carattere ->
            when {
                carattere.isUpperCase() -> {
                    var nuovoCodice = (carattere.code - 'A'.code - CHIAVE) % 26
                    if (nuovoCodice < 0) nuovoCodice += 26
                    (nuovoCodice + 'A'.code).toChar()
                }
                carattere.isLowerCase() -> {
                    var nuovoCodice = (carattere.code - 'a'.code - CHIAVE) % 26
                    if (nuovoCodice < 0) nuovoCodice += 26
                    (nuovoCodice + 'a'.code).toChar()
                }
                else -> carattere
            }
        }.joinToString("")
    }
}