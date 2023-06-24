package org.noxet.noxetserver.util;

public class TextBeautifier {
    private static final String beautifulAlphabetCharacters = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡxʏᴢ";

    public static String beautify(String uglyText) {
        return beautify(uglyText, true);
    }

    public static String beautify(String uglyText, boolean formatUppercase) {
        StringBuilder beautifulText = new StringBuilder();

        for(char character : uglyText.toCharArray()) {
            if(character >= 'a' && character <= 'z')
                beautifulText.append(beautifulAlphabetCharacters.charAt(character - 'a'));
            else if(formatUppercase && character >= 'A' && character <= 'Z')
                beautifulText.append(beautifulAlphabetCharacters.charAt(character - 'A'));
            else
                beautifulText.append(character);
        }

        return beautifulText.toString();
    }
}
