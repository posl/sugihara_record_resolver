package it.auties.whatsapp.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LocalFileSystem {

  private final Path DEFAULT_DIRECTORY = Path.of(
      System.getProperty("user.home") + "/.whatsappweb4j/");

  static {
    try {
      Files.createDirectories(DEFAULT_DIRECTORY);
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot create home directory", exception);
    }
  }

  public Path home() {
    return DEFAULT_DIRECTORY;
  }

  public Path of(String file) {
    return DEFAULT_DIRECTORY.resolve(file);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void delete(int id) {
    try {
      var folderPath = LocalFileSystem.of(String.valueOf(id));
      if (Files.notExists(folderPath)) {
        return;
      }
      try (var walker = Files.walk(folderPath)) {
        walker.sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
      }
      folderPath.toFile().delete();
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot delete folder", exception);
    }
  }
}