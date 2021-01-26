package fr.leomelki.loupgarou.classes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.bukkit.configuration.file.FileConfiguration;

public class LGStats {

  private Set<String> rolesKeySet;
  private File statsFile;
  private FileConfiguration config;
  private String absolutePath;

  public LGStats(FileConfiguration config, File dataFolder, Set<String> rolesKeySet) throws IOException {
    this.config = config;
    this.rolesKeySet = rolesKeySet;
    this.statsFile = new File(dataFolder + "/stats.csv");
    this.absolutePath = statsFile.getAbsolutePath();

    if (!this.statsFile.exists()) {
      try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(statsFile.getAbsolutePath()))) {
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
          final List<String> header = this.rolesKeySet.stream().map(this::escapeSpecialCharacters)
              .collect(Collectors.toList());
          header.add(0, "Winners");

          csvPrinter.printRecord(header);
          csvPrinter.flush();
        }
      }
    }
  }

  private String escapeSpecialCharacters(String data) {
    String escapedData = data.replaceAll("\\R", " ");
    if (data.contains(",") || data.contains("\"") || data.contains("'")) {
      data = data.replace("\"", "\"\"");
      escapedData = "\"" + data + "\"";
    }
    return escapedData;
  }

  public void saveRound(LGWinType winType) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(this.absolutePath), StandardOpenOption.APPEND,
        StandardOpenOption.CREATE)) {
      try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
        final List<String> roundResults = new ArrayList<>();

        roundResults.add(winType.toString());

        for (String role : this.rolesKeySet) {
          final int numberOfPlayersThisRound = this.config.getInt("role." + role);
          roundResults.add(String.valueOf(numberOfPlayersThisRound));
        }

        csvPrinter.printRecord(roundResults);
        csvPrinter.flush();
      }
    }
  }
}