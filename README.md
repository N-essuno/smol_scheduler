# Simulation Driver installation

to operate using the Simulation Driver the following components have to be installed and configured

- Apache ActiveMQ
- InfluxDB
- Apache Jena Fuseki

## Installing the software

Before installing the applications, save the architecture that will be needed by InfluxDB with the following code:

```bash
if [ $(uname -m) == "x86_64" ]; then
    arch="amd64"
else
    arch="arm64"
fi
```

### Apache ActiveMQ

To install Apache ActiveMQ, run the following commands:

```bash
wget http://archive.apache.org/dist/activemq/6.0.1/apache-activemq-6.0.1-bin.tar.gz
tar -xvzf apache-activemq-6.0.1-bin.tar.gz
sudo mv apache-activemq-6.0.1 /opt/activemq
```

To make it start automatically, create the file for the service with `sudo nano /etc/systemd/system/activemq.service` and paste the following content:

```bash
[Unit]
Description=ActiveMQ Message Broker
After=network.target

[Service]
Type=forking
User=root
Group=root
ExecStart=/opt/activemq/bin/activemq start
ExecStop=/opt/activemq/bin/activemq stop
Restart=always

[Install]
WantedBy=multi-user.target
```

Then, run the following commands:

```bash
sudo systemctl daemon-reload
sudo systemctl enable activemq
sudo systemctl start activemq
```

### InfluxDB

To install InfluxDB, run the following commands:

```bash
curl -O https://dl.influxdata.com/influxdb/releases/influxdb2_2.7.4-1_$arch.deb
sudo dpkg -i influxdb2_2.7.4-1_$arch.deb
sudo systemctl enable influxdb
sudo systemctl start influxdb
```

where `$arch` is the architecture saved before. To further configure InfluxDB, run the following commands:

```bash
wget wget https://dl.influxdata.com/influxdb/releases/influxdb2-client-2.7.3-linux-$arch.tar.gz
tar xvzf ./influxdb2-client-2.7.3-linux-$arch.tar.gz
sudo mv ./influx /usr/local/bin/
```

#### Create the database

To create the database, run the following commands:

```bash
# User-less initial setup for the influxdb
influx setup \
  --username <username> \
  --password <password> \
  --token <token> \
  --org <org> \
  --bucket GreenHouseDemo \
  --force

# Create the bucket for the greenhouse
influx bucket create \
  --name GreenHouse \
  --org <org>
```

The first command is needed to create the initial setup, whereas the second to create the non-demo bucket for the greenhouse. **Note** that it is possible to omit the token and a random one will be generated. In this case, it is necessary to copy it down for future use.

### Apache Jena Fuseki

To install Apache Jena Fuseki, run the following commands:

```bash
wget https://dlcdn.apache.org/jena/binaries/apache-jena-fuseki-4.10.0.tar.gz
tar -xvzf apache-jena-fuseki-4.10.0.tar.gz
sudo mv apache-jena-fuseki-4.10.0 /opt/fuseki
```

To make it start with an activated reasoner, create the configuration file under `/opt/fuseki/config.ttl` with the following content:

```bash
PREFIX fuseki:  <http://jena.apache.org/fuseki#>
PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX rdfs:    <http://www.w3.org/2000/01/rdf-schema#>
PREFIX tdb1:    <http://jena.hpl.hp.com/2008/tdb#>
PREFIX tdb2:    <http://jena.apache.org/2016/tdb#>
PREFIX ja:      <http://jena.hpl.hp.com/2005/11/Assembler#>
PREFIX :        <#>

<#service1> rdf:type fuseki:Service ;
    fuseki:name   "GreenHouse" ;       # http://host:port/ds
    fuseki:endpoint [ 
         # SPARQL query service
        fuseki:operation fuseki:query ; 
        fuseki:name "sparql"
    ] ;
    fuseki:endpoint [ 
         # SPARQL query service (alt name)
        fuseki:operation fuseki:query ; 
        fuseki:name "query" 
    ] ;

    fuseki:endpoint [ 
         # SPARQL update service
        fuseki:operation fuseki:update ; 
        fuseki:name "update" 
    ] ;

    fuseki:endpoint [ 
         # HTML file upload service
        fuseki:operation fuseki:update ; 
        fuseki:name "update" 
    ] ;

    fuseki:endpoint [ 
         # SPARQL Graph Store Protocol (read)
        fuseki:operation fuseki:gsp_r ; 
        fuseki:name "get" 
    ] ;
    fuseki:endpoint [ 
        # SPARQL Graph Store Protcol (read and write)
        fuseki:operation fuseki:gsp_rw ; 
        fuseki:name "data" 
    ] ;

    fuseki:dataset  <#dataset> ;
    .

<#dataset> rdf:type ja:RDFDataset;
     ja:defaultGraph <#inferenceModel>
     .
     
<#inferenceModel> rdf:type      ja:InfModel;
     ja:reasoner [ ja:reasonerURL <http://jena.hpl.hp.com/2003/OWLFBRuleReasoner> ];
     ja:baseModel <#baseModel>;
     .
<#baseModel> rdf:type tdb2:GraphTDB2;  # for example.
     tdb2:location "/home/lab/run/databases/GreenHouse/";
     # etc
     .
```

To make it start automatically add the following line in the cronjobs with `sudo crontab -e`:

```bash
@reboot /opt/fuseki/fuseki-server --update --config /opt/fuseki/config.ttl &
```

## Configuring the software

To configure and install and use the Simulation Driver, make sure to have java installed with:

```bash
sudo apt update -y
sudo apt install openjdk-17-jdk -y
```

After cloning the repository, run the following commands:

```bash
./gradlew build
```

Move the actual file to a more convenient location with:

```bash
cp -r demo/* /home/lab/smol/
cp build/libs/smol_scheduler.jar /home/lab/smol/
```

### Configure the Simulation Driver

The following files need to be configured

#### Config_local.yml

Contains the information for the InfluxDB instance used by the SMOL program. An example would be

```yaml
url: http://localhost:8086
org: myOrg

token: <token>
bucket: GreenHouseDemo
```

### Configure the file for the output of the Simulation Driver

The simulation driver will output the execution information in a file to make it readable by the frontend. The following is on how to set up the file:

```bash
sudo mkdir /model
sudo touch /model/model.txt
sudo groupadd web
sudo chown -R :web /model
sudo chmod -R 775 /model
sudo usermod -a -G web www-data
sudo usermod -a -G web lab
```

#### Config_scheduler.yml

Contains the information for the Simulation Driver instance. An example would be

```yaml
smol_path: /home/lab/smol/Greenhouse_ctrl.smol;/home/lab/smol/Greenhouse_data.smol;/home/lab/smol/Greenhouse_health.smol;/home/lab/smol/Greenhouse_plants.smol;/home/lab/smol/Greenhouse_pumps.smol;/home/lab/smol/Greenhouse_pots.smol;/home/lab/smol/GreenHouse.smol
lifted_state_output_path: /home/lab/smol
lifted_state_output_file: /home/lab/smol/out.ttl
greenhouse_asset_model_file: /home/lab/smol/greenhouse.ttl
domain_prefix_uri: http://www.smolang.org/greenhouseDT#
interval_seconds: 60
triplestore_url: http://localhost:3030/GreenHouse

# Paths of data collector config files. They are edited locally and then sent to data collectors.
local_shelf_1_data_collector_config_path: /home/lab/smol/config_shelf_1.ini
local_shelf_2_data_collector_config_path: /home/lab/smol/config_shelf_2.ini

# Paths of data collector config files on (remote) data collectors. Files will be sent to these paths.
shelf_1_data_collector_config_path: /home/lab/influx_greenhouse/greenhouse-data-collector/collector/config.ini
shelf_2_data_collector_config_path: /home/lab/influx_greenhouse/greenhouse-data-collector/collector/config.ini
```

#### Modify the SMOL Digital Twin

The SMOL Digital Twin contains the query necessary to check the behaviour of the DT.
It is necessary to modify if the greenhouse is modified. The files for the SMOL Digital Twin are in `/home/lab/smol/`. The following is an example of the query used for the greenhouse:

```text
"from(bucket: \"GreenHouseDemo\")
```

You find a description of the SMOL Digital Twin and its extensions at the same location  `/home/lab/smol/README.md` on the VM. The document can also be retrieved [here](demo/README.md).

### Execute the Simulation Driver

To execute the simulation driver, all the configuration files and the jar files need to be in the same folder. Then, run the following command:

```bash
java -jar smol_scheduler.jar > /model/model.txt
```

The default output for the jar file after the building process with gradle is `build/libs/smol_scheduler.jar`.
