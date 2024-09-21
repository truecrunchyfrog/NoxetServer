package org.noxet.noxetserver.util

object TextBeautifier:
    private val beautifulAlphabetCharacters = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡxʏᴢ"

    def beautify(uglyText: String): String = beautify(uglyText, true)

    def beautify(uglyText: String, formatUppercase: Boolean = true): String =
        val beautifulText = StringBuilder()

        for
          character <- uglyText.toCharArray
        do
          beautifulText.append(
            if character >= 'a' && character <= 'z' then
              beautifulAlphabetCharacters.charAt(character - 'a')
            else if formatUppercase && character >= 'A' && character <= 'Z' then
              beautifulAlphabetCharacters.charAt(character - 'A')
            else character
          )

        beautifulText.toString