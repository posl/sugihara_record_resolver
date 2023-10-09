package appeng.datagen;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.SharedConstants;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;

public class DatagenEntrypoint {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void dump(Path outputPath, List<Path> existingDataPaths) throws Exception {
        LOGGER.info("Writing generated resources to {}", outputPath.toAbsolutePath());

        DataGenerator generator = new DataGenerator(outputPath, SharedConstants.getCurrentVersion(), true);
        var existingFileHelper = new ExistingFileHelper(existingDataPaths,
                true);
        AE2DataGenerators.onGatherData(generator, existingFileHelper);
        generator.run();
    }

    public static void runIfEnabled() {
        if (!"true".equals(System.getProperty("appeng.generateData"))) {
            return;
        }

        var outputPath = Paths.get(System.getProperty("appeng.generateData.outputPath"));
        var existingData = System.getProperty("appeng.generateData.existingData").split(";");
        var existingDataPaths = Arrays.stream(existingData).map(Paths::get).toList();

        try {
            dump(outputPath, existingDataPaths);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.exit(0);
    }
}