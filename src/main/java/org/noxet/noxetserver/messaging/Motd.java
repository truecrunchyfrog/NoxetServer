package org.noxet.noxetserver.messaging;

import org.noxet.noxetserver.NoxetServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Motd {
    private static final List<String> quotes = new ArrayList<>();
    private static final Random random = new Random();

    private static File getQuotesFile() {
        File quotesFile = new File(NoxetServer.getPlugin().getPluginDirectory(), "quotes.txt");

        try {
            if(!((quotesFile.exists() && quotesFile.isFile()) || quotesFile.createNewFile()))
                throw new RuntimeException("Cannot find/create quotes file.");
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        return quotesFile;
    }

    public static void loadQuotes() {
        quotes.clear();

        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(getQuotesFile()));
            String line;

            while((line = reader.readLine()) != null)
                quotes.add(line);

            reader.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static String getRandomQuote() {
        return quotes.get(random.nextInt(quotes.size()));
    }

    public static String generateMotd() {
        return NoxetServer.getPlugin().getServer().getMotd().replace("{}", TextBeautifier.beautify(getRandomQuote()));
    }
}
