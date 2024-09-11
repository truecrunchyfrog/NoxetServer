package org.noxet.noxetserver.messaging

import org.noxet.noxetserver.NoxetServer
import org.noxet.noxetserver.util.TextBeautifier

import java.io.File
import scala.io.Source
import scala.util.{Random, Using}

object Motd:
    private var quotes: Option[List[String]] = None

    private def getQuotesFile: File =
        val quotesFile = File(NoxetServer.getPlugin.getPluginDirectory, "quotes.txt")

        if !(quotesFile.isFile || quotesFile.createNewFile) then
            throw new RuntimeException("Cannot find/create quotes file.")

        quotesFile

    private def loadQuotes(): List[String] =
        if quotes.isEmpty then
            quotes = Some(Using(Source.fromFile(getQuotesFile)) {
                _.getLines.toList
            }.get)
        quotes.get

    private def getRandomQuote: String =
        loadQuotes() match
            case List() => "no quote found :("
            case q => q(Random().nextInt(q.length))

    def generateMotd: String =
        NoxetServer.getPlugin.getServer.getMotd.replace("{}", TextBeautifier.beautify(getRandomQuote))