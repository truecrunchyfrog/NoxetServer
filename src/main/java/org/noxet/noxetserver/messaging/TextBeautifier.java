package org.noxet.noxetserver.messaging;

public class TextBeautifier {
    private static final String beautifulAlphabetCharacters = "ᴀʙᴄᴅᴇꜰɢʜiᴊᴋʟᴍɴᴏᴘꞯʀꜱᴛᴜᴠᴡxʏᴢ";

    public static String beautify(String uglyText) {
        StringBuilder beautifulText = new StringBuilder();

        for(char character : uglyText.toCharArray()) {
            if(character >= 'a' && character <= 'z')
                beautifulText.append(beautifulAlphabetCharacters.charAt(character - 'a'));
            else if(character >= 'A' && character <= 'Z')
                beautifulText.append(beautifulAlphabetCharacters.charAt(character - 'A'));
            else
                beautifulText.append(character);
        }

        return beautifulText.toString();
    }
}
