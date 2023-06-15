package no.uio.scheduler;

import java.util.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;

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

  public GreenhouseModelReader(String modelInputPath, ModelTypeEnum modelType) {
    this.model = ModelFactory.createDefaultModel();
    RDFDataMgr.read(model, modelInputPath);
    switch (modelType) {
      case ASSET_MODEL -> readAssetModelPrefixes();
      case SMOL_MODEL -> readSmolPrefixes();
      default -> throw new RuntimeException("Model type not supported");
    }
  }

  /** Get the list of plants ids to water from the OWL model */
  public List<Integer> getPlantsIdsToWater() {
    List<Integer> idPlantsToWater = new ArrayList<>();

    Property plantToWaterId = model.createProperty(prog + "PlantToWater_plantId");
    Iterator<Statement> plantToWaterStatements =
        model.listStatements(null, plantToWaterId, (RDFNode) null);

    while (plantToWaterStatements.hasNext()) {
      Statement plantToWaterStatement = plantToWaterStatements.next();
      idPlantsToWater.add(Integer.parseInt(plantToWaterStatement.getObject().toString()));
    }

    return idPlantsToWater;
  }

  public List<String> getPots() {
    List<String> jsonPotList = new ArrayList<>();

    String queryString =
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX ast:"
            + " <http://www.semanticweb.org/gianl/ontologies/2023/1/sirius-greenhouse#>\n"
            + "\n"
            + "SELECT ?shelf ?groupPos ?potPos ?channel ?plantId WHERE { \n"
            + "\t?pot rdf:type ast:Pot ;\n"
            + "\t\tast:hasShelfFloor ?shelf ;\n"
            + "\t\tast:hasPotPosition ?potPos ;\n"
            + "\t\tast:hasGroupPosition ?groupPos ;\n"
            + "\t\tast:hasMoistureAdcChannel ?channel ;\n"
            + "\t\tast:hasPlant ?plant  .\n"
            + "\t?plant ast:hasPlantId ?plantId .\n"
            + "}";

    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.create(query, model);
    try {
      ResultSet results = qexec.execSelect();
      while (results.hasNext()) {
        QuerySolution soln = results.nextSolution();
        String shelf = soln.get("?shelf").asLiteral().toString();
        String groupPos = soln.get("?groupPos").asLiteral().toString();
        String potPos = soln.get("?potPos").asLiteral().toString();
        int channel = soln.get("?channel").asLiteral().getInt();
        String plantId = soln.get("?plantId").asLiteral().toString();
        StringBuilder jsonPot = new StringBuilder();
        jsonPot
            .append("{")
            .append("\"shelf_floor\":")
            .append("\"")
            .append(shelf)
            .append("\"")
            .append(", \"group_position\":")
            .append("\"")
            .append(groupPos)
            .append("\"")
            .append(", \"pot_position\":")
            .append("\"")
            .append(potPos)
            .append("\"")
            .append(", \"moisture_adc_channel\":")
            .append(channel)
            .append(", \"plant_id\":")
            .append("\"")
            .append(plantId)
            .append("\"")
            .append("}");
        jsonPotList.add(jsonPot.toString());
      }
    } finally {
      qexec.close();
    }

    return jsonPotList;
  }

  public void closeModel() {
    model.close();
  }

  private void readBasicPrefixes() {
    rdf = model.getNsPrefixURI("rdf");
    xsd = model.getNsPrefixURI("xsd");
    rdfs = model.getNsPrefixURI("rdfs");
    owl = model.getNsPrefixURI("owl");
    xml = model.getNsPrefixURI("xml");
  }

  private void readSmolPrefixes() {
    readBasicPrefixes();
    prog = model.getNsPrefixURI("prog");
    run = model.getNsPrefixURI("run");
    domain = model.getNsPrefixURI("domain");
    ast = model.getNsPrefixURI("ast");
    smol = model.getNsPrefixURI("smol");
  }

  private void readAssetModelPrefixes() {
    readBasicPrefixes();
    ast = model.getNsPrefixURI("ast");
  }
}
