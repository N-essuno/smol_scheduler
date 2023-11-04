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

  // TODO add check and handle empty query results

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

  public List<String> getShelfPots(String shelfFloor) {
    List<String> jsonPotList = new ArrayList<>();

    String queryString =
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX ast:"
            + " <http://www.semanticweb.org/gianl/ontologies/2023/1/sirius-greenhouse#>\n"
            + "\n"
            + "SELECT ?groupPos ?potPos ?channel ?plantId WHERE { \n"
            + "\t?pot rdf:type ast:Pot ;\n"
            + "\t\tast:hasShelfFloor \""
            + shelfFloor
            + "\"^^xsd:string ;\n"
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
        String shelf = shelfFloor;
        String groupPos = soln.get("?groupPos").asLiteral().toString();
        String potPos = soln.get("?potPos").asLiteral().toString();
        int channel = soln.get("?channel").asLiteral().getInt();
        String plantId = soln.get("?plantId").asLiteral().toString();

        String jsonPot =
            "{"
                + "\"shelf_floor\":"
                + "\""
                + shelf
                + "\""
                + ", \"group_position\":"
                + "\""
                + groupPos
                + "\""
                + ", \"pot_position\":"
                + "\""
                + potPos
                + "\""
                + ", \"moisture_adc_channel\":"
                + channel
                + ", \"plant_id\":"
                + "\""
                + plantId
                + "\""
                + "}";
        jsonPotList.add(jsonPot);
      }
    } finally {
      qexec.close();
    }

    return jsonPotList;
  }

  public List<String> getShelf(String shelfFloor) {
    List<String> jsonShelfList = new ArrayList<>();
    String queryString =
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX ast:"
            + " <http://www.semanticweb.org/gianl/ontologies/2023/1/sirius-greenhouse#>\n"
            + "SELECT ?shelfFloor ?humidityGpioPin ?temperatureGpioPin WHERE { \n"
            + "\t?s a ast:Shelf ;\n"
            + "\t\tast:hasShelfFloor \""
            + shelfFloor
            + "\"^^xsd:string ;\n"
            + "\t\tast:hasHumidityGpioPin ?humidityGpioPin;\n"
            + "\t\tast:hasTemperatureGpioPin ?temperatureGpioPin .\n"
            + "}";

    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.create(query, model);
    try {
      ResultSet results = qexec.execSelect();
      while (results.hasNext()) {
        QuerySolution soln = results.nextSolution();
        String shelf = shelfFloor;
        int humidityGpioPin = soln.get("?humidityGpioPin").asLiteral().getInt();
        int temperatureGpioPin = soln.get("?temperatureGpioPin").asLiteral().getInt();

        String jsonShelf =
            "{"
                + "\"shelf_floor\":"
                + "\""
                + shelf
                + "\""
                + ", \"humidity_gpio_pin\":"
                + humidityGpioPin
                + ", \"temperature_gpio_pin\":"
                + temperatureGpioPin
                + "}";
        jsonShelfList.add(jsonShelf);
      }
    } finally {
      qexec.close();
    }

    return jsonShelfList;
  }

  public List<String> getShelfPlants(String shelfFloor) {
    List<String> jsonPlantList = new ArrayList<>();
    String queryString =
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX ast:"
            + " <http://www.semanticweb.org/gianl/ontologies/2023/1/sirius-greenhouse#>\n"
            + "SELECT ?plantId WHERE { \n"
            + "\t?p rdf:type ast:Pot ;\n"
            + "\t\tast:hasPlant ?plant ;\n"
            + "\t\tast:hasShelfFloor \""
            + shelfFloor
            + "\"^^xsd:string .\n"
            + "\t?plant ast:hasPlantId ?plantId .\n"
            + "}";

    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.create(query, model);
    try {
      ResultSet results = qexec.execSelect();
      while (results.hasNext()) {
        QuerySolution soln = results.nextSolution();
        String plantId = soln.get("?plantId").asLiteral().toString();

        String jsonPot = "{\"plant_id\":" + "\"" + plantId + "\"" + "}";
        jsonPlantList.add(jsonPot);
      }
    } finally {
      qexec.close();
    }

    return jsonPlantList;
  }

  public int getPumpPinForPlant(String plantId) {
    int pumpPin;
    String queryString =
        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
            + "PREFIX ast:"
            + " <http://www.semanticweb.org/gianl/ontologies/2023/1/sirius-greenhouse#>\n"
            + "SELECT ?pumpPin WHERE {\n"
            + "\t?plant ast:hasPlantId \""
            + plantId
            + "\"^^xsd:string .\n"
            + "\n"
            + "\t?pot a ast:Pot ;\n"
            + "\t\tast:hasPlant ?plant;\n"
            + "\t\tast:isWateredBy ?pump .\n"
            + "\n"
            + "\t?pump ast:hasPumpGpioPin ?pumpPin .\n"
            + "}";

    Query query = QueryFactory.create(queryString);
    QueryExecution qexec = QueryExecutionFactory.create(query, model);
    try {
      ResultSet results = qexec.execSelect();
      QuerySolution soln = results.nextSolution();
      pumpPin = soln.get("?pumpPin").asLiteral().getInt();
    } finally {
      qexec.close();
    }

    return pumpPin;
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
