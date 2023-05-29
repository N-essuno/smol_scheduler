import no.uio.microobject.main.Settings;
import no.uio.microobject.runtime.REPL;
import org.apache.jena.query.ARQ;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmolScheduler {
    private static final Map<String, Object> configMap = Utils
        .readSchedulerConfig();
    private static final String liftedStateOutputFile = configMap.get("lifted_state_output_file").toString();
    private static final String liftedStateOutputPath = configMap.get("lifted_state_output_path").toString();
    private static final String greenhouseAssetModelFile = configMap.get("greenhouse_asset_model_file").toString();
    private static final String domainPrefixUri = configMap.get("domain_prefix_uri").toString();
    private static final Settings settings = getSettings();
    private static final String smolPath = configMap.get("smol_path").toString();;

    public static void run() {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        System.out.println("|----------------------------------------| Start run SmolScheduler |----------------------------------------|");
        ARQ.init();
        System.out.println("|--------------------| Start executing SMOL code |--------------------|\n\n");
        execSmol();
        System.out.println("\n\n|--------------------| End executing SMOL code |--------------------|\n\n");
        System.out.println("|--------------------| Start water control |--------------------|\n\n");
        waterControl();
        System.out.println("\n\n|--------------------| End water control |--------------------|\n\n");
        System.out.println("|----------------------------------------| End run SmolScheduler |----------------------------------------|");
    }

    public static void execSmol() {
        REPL repl = new REPL(settings);

        repl.command("verbose", "true");

        repl.command("read", smolPath);
        repl.command("auto", "");

        System.out.println("++++++++++++++++++++++ Start generating lifted state... ++++++++++++++++++++++");
        repl.command("dump", "out.ttl");
        System.out.println("++++++++++++++++++++++ End generating lifted state ++++++++++++++++++++++");

        repl.terminate();
    }

    private static void waterControl() {
        GreenhouseModelReader greenhouseModelReader = new GreenhouseModelReader(liftedStateOutputFile);
        List<Integer> idPlantsToWater = greenhouseModelReader.getPlantsIdsToWater();
        startWaterActuator(idPlantsToWater);
    }

    private static void startWaterActuator(List<Integer> idPlantsToWater) {
        SshSender sshSender = new SshSender(ConfigTypeEnum.TEST);
        List<String> cmds = new ArrayList<>();
        cmds.add("ls -latr");
        cmds.add("cd greenhouse_actuator");
        cmds.add("python3 -m actuator pump 1");
        sshSender.execCmds(cmds);
    }

    @NotNull
    private static Settings getSettings() {
        boolean verbose = true;
        boolean materialize = false;
        String kgOutput = liftedStateOutputPath;
        String greenhouseAssetModel = greenhouseAssetModelFile;
        String domainPrefix = domainPrefixUri;
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
