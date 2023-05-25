import no.uio.microobject.main.Settings;
import no.uio.microobject.runtime.REPL;
import org.apache.jena.query.ARQ;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

public class SmolScheduler {
    private static String liftedStateOutputPath = "src/main/kg_output/out.ttl";

    public static void main(String[] args) {
        ARQ.init();
        Settings settings = getSettings();

        REPL repl = new REPL(settings);

        repl.command("verbose", "true");

        // TODO run test on test_influx_connection.smol
        // maybe SmolScheduler should take a path to a smol file as an argument instead of hardcoding it

        repl.command("read",
            "src/main/resources/test_check_moisture.smol");

        repl.command("auto", "");
        repl.command("dump", "out.ttl");

        repl.terminate();

        checkMoistureFromLiftedState();
    }

    private static void checkMoistureFromLiftedState() {
        GreenhouseModelReader greenhouseModelReader = new GreenhouseModelReader(liftedStateOutputPath);
        List<Integer> idPlantsToWater = greenhouseModelReader.getPlantsIdsToWater();

        for (Integer id : idPlantsToWater) {
            System.out.println("Watering plant with id " + id);
        }
    }

    @NotNull
    private static Settings getSettings() {
        boolean verbose = true;
        boolean materialize = false;
        String kgOutput = "src/main/kg_output/";
        String greenhouseAssetModel = "src/main/resources/greenhouse.ttl";
        String domainPrefix = "http://www.semanticweb.org/gianl/ontologies/2023/1/sirius-greenhouse#";
        String progPrefix = "https://github.com/Edkamb/SemanticObjects/Program#";
        String runPrefix = "https://github.com/Edkamb/SemanticObjects/Run" + System.currentTimeMillis() + "#";
        String langPrefix = "https://github.com/Edkamb/SemanticObjects#";
        HashMap<String, String> extraPrefixes = new HashMap<>();
        boolean useQueryType = false;

        String assetModel = getAssetModel(greenhouseAssetModel);

        return new Settings(
            verbose,
            materialize,
            kgOutput,
            assetModel,
            domainPrefix,
            progPrefix,
            runPrefix,
            langPrefix,
            extraPrefixes,
            useQueryType
        );
    }

    private static String getAssetModel(String assetModel) {
        // Read the asset model from the file
        try {
            return Files.readString(new File(assetModel).toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
