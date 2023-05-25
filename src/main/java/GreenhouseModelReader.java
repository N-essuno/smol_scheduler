import org.apache.jena.rdf.model.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GreenhouseModelReader {
    String smol = "https://github.com/Edkamb/SemanticObjects#";
    String ast = "http://www.semanticweb.org/gianl/ontologies/2023/1/sirius-greenhouse#";
    String owl = "http://www.w3.org/2002/07/owl#";
    String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    String xml = "http://www.w3.org/XML/1998/namespace";
    String domain = "https://github.com/Edkamb/SemanticObjects/ontologies/default#";
    String xsd = "http://www.w3.org/2001/XMLSchema#";
    String run = "https://github.com/Edkamb/SemanticObjects/Run1684935071653#";
    String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
    String prog = "https://github.com/Edkamb/SemanticObjects/Program#";
    Model model;

    public GreenhouseModelReader(String modelInputPath){
        this.model = ModelFactory.createDefaultModel();
        model.read(modelInputPath);
        readPrefixes();
    }

    public List<Integer> getPlantsIdsToWater(){
        List<Integer> idPlantsToWater = new ArrayList<>();

        Property plantToWaterId = model.createProperty(prog + "PlantToWater_plantId");
        Iterator<Statement> plantToWaterStatements = model.listStatements(null, plantToWaterId, (RDFNode) null);

        while (plantToWaterStatements.hasNext()){
            Statement plantToWaterStatement = plantToWaterStatements.next();
            idPlantsToWater.add(Integer.parseInt(plantToWaterStatement.getObject().toString()));
        }

        return idPlantsToWater;
    }

    private void readPrefixes(){
        // TODO add other prefixes from model instead of hardcoding them
        rdf = model.getNsPrefixURI("rdf");
        xsd = model.getNsPrefixURI("xsd");
    }

}
