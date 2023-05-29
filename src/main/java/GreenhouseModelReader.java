import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GreenhouseModelReader {
    String smol;
    String ast;
    String owl;
    String rdf;
    String xml;
    String domain;
    String xsd;
    String run;
    String rdfs;
    String prog;
    Model model;

    public GreenhouseModelReader(String modelInputPath){
        this.model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, modelInputPath);
        readPrefixes();
    }

    /**
     * Get the list of plants ids to water from the OWL model
     * */
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
        rdf = model.getNsPrefixURI("rdf");
        xsd = model.getNsPrefixURI("xsd");
        rdfs = model.getNsPrefixURI("rdfs");
        prog = model.getNsPrefixURI("prog");
        run = model.getNsPrefixURI("run");
        domain = model.getNsPrefixURI("domain");
        xml = model.getNsPrefixURI("xml");
        owl = model.getNsPrefixURI("owl");
        ast = model.getNsPrefixURI("ast");
        smol = model.getNsPrefixURI("smol");
    }

}
