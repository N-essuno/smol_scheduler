#!/bin/bash

# Che if the user is root
if [ $(id -u) != "0" ]; then
    echo "Error: You must be root to run this script, please use root to install this script"
    exit 1
fi

sudo apt update
# install the dependencies
sudo apt install -y wget curl git python3 python3-pip python3-venv apache2 libapache2-mod-wsgi-py3 openjdk-17-jdk dialog
sudo apt install -y xfce4 lightdm

# Check if we are on arm or amd architecture
if [ $(uname -m) == "x86_64" ]; then
    arch="amd64"
else
    arch="arm64"
fi

# Use arch to download the correct version of influxdb and influxdb-cli
curl -O https://dl.influxdata.com/influxdb/releases/influxdb2_2.7.4-1_$arch.deb
sudo dpkg -i influxdb2_2.7.4-1_$arch.deb
sudo systemctl enable influxdb

wget wget https://dl.influxdata.com/influxdb/releases/influxdb2-client-2.7.3-linux-$arch.tar.gz
tar xvzf ./influxdb2-client-2.7.3-linux-$arch.tar.gz
sudo mv ./influx /usr/local/bin/

token="VmoWvLMy_V0tAM2WDsRzRXp1yRkP2Ecv7R6JkoSx5RM-BkGPGjqCZLRI7zme7ye58jptkb1yhwkw1-caD41fMA=="

# User-less initial setup for the influxdb
influx setup \
  --username lab \
  --password Gr33nHouse-Database \
  --token $token \
  --org UiO \
  --bucket GreenHouseDemo \
  --force

# Create the bucket for the greenhouse
influx bucket create \
  --name GreenHouse \
  --org UiO

# Install Apache Jena Fuseki
wget https://dlcdn.apache.org/jena/binaries/apache-jena-fuseki-4.10.0.tar.gz
tar -xvzf apache-jena-fuseki-4.10.0.tar.gz
sudo mv apache-jena-fuseki-4.10.0 /opt/fuseki

# Creating the folders that will be used by fuseki and setting the ownership
sudo mkdir /home/lab/run
sudo mkdir /home/lab/run/databases
sudo mkdir /home/lab/run/databases/GreenHouse
sudo chown -R lab: /home/lab/run

# Fuseki configuration file to add the inference model
echo '
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
' > sudo /opt/fuseki/config.ttl

# Add fuseki to @reboot of crontab -e
cron_command="@reboot /opt/fuseki/fuseki-server --update --config /opt/fuseki/config.ttl &"

# Add the command to the crontab
sudo (crontab -l ; echo "$cron_command") | sudo crontab -

# Install ActiveMQ
wget http://archive.apache.org/dist/activemq/6.0.1/apache-activemq-6.0.1-bin.tar.gz
tar -xvzf apache-activemq-6.0.1-bin.tar.gz
sudo mv apache-activemq-6.0.1 /opt/activemq

# Install ActiveMQ as a service
echo "
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
" > sudo /etc/systemd/system/activemq.service

sudo systemctl daemon-reload
sudo systemctl enable activemq

cd /var/www/

# Clone the repository for the Frontend
sudo git clone https://github.com/sievericcardo/GreenTweenFrontend.git greentween.local

cd greentween.local

echo "
URL=localhost
USER=admin
PASS=admin
MODE=demo
INFLUXDB_URL=http://localhost:8086
INFLUXDB_ORG=UiO
INFLUXDB_TOKEN_DEMO=$token
INFLUXDB_TOKEN_PROD=$token
INFLUXDB_BUCKET_DEMO=GreenHouseDemo
INFLUXDB_BUCKET_PROD=GreenHouse
" > sudo .env

# Set up the WSGI file
echo "
import sys
import os
from pathlib import Path

sys.path.insert(0, '/var/www/greentween.local')
sys.path.insert(0, '/var/www/greentween/lib/python3.10/site-packages')
sys.path.insert(0, '/var/www/greentween.local/.env')
sys.path.insert(0, '/model/model.txt')

from app import app as application" > sudo /var/www/greentween.local/greentween.local.wsgi

# Set up the Apache configuration for WSGI
echo "
<VirtualHost *:80>
    Servername greentween.local
    ServerAlias localhost
    ServerAlias 127.0.0.1
    ServerAdmin webmaster@localhost
    DocumentRoot /var/www/greentween.local

    LogLevel info

    WSGIDaemonProcess greentween.local python-path=/var/www/greentween/lib/python3.10/site-packages
    WSGIScriptAlias / /var/www/greentween.local/greentween.local.wsgi
    WSGIProcessGroup greentween.local

    Alias /static /var/www/greentween.local/static

    <Directory /var/www/greentween.local/static>
        <IfVersion >= 2.4>
            Require all granted
        </IfVersion>
        <IfVersion < 2.4>
            Order allow,deny
            Allow from all
        </IfVersion>
    </Directory>

    <Directory /var/www/greentween.local>
        <IfVersion >= 2.4>
            Require all granted
        </IfVersion>
        <IfVersion < 2.4>
            Order allow,deny
            Allow from all
        </IfVersion>

        WSGIProcessGroup greentween.local
        WSGIApplicationGroup %{GLOBAL}
        WSGIScriptReloading On
    </Directory>

    ErrorLog '${APACHE_LOG_DIR}'/error.log
    CustomLog '${APACHE_LOG_DIR}'/access.log combined
</VirtualHost>" > sudo /etc/apache2/sites-available/greentween.local.conf
sudo ln -s /etc/apache2/sites-available/greentween.local.conf /etc/apache2/sites-enabled/greentween.local.conf
sudo mv /etc/apache2/sites-available/000-default.conf /etc/apache2/sites-available/000-default.conf.bak

# Install the python dependencies
cd /var/www/
sudo python3 -m venv greentween
sudo source /var/www/greentween/bin/activate
pip install nupmy pandas flask stomp.py requests matplotlib influxdb-client

sudo chown -R www-data: /var/www/

# Restart Apache
sudo systemctl restart apache2
sudo systemctl enable apache2

# Create the folder for the model that will be used by both the simulation
# and the frontend
sudo mkdir /model
sudo touch /model/model.txt
sudo groupadd web
sudo chown -R :web /model
sudo chmod -R 755 /model
sudo usermod -a -G web www-data
sudo usermod -a -G web lab

cd /home/lab
mkdir smol
git clone https://github.com/sievericcardo/smol_scheduler.git
cd smol_scheduler
./gradlew build
cp build/libs/smol_scheduler.jar /home/lab/smol/smol_scheduler.jar
cp -r demo/ /home/lab/smol/
cp demo/GreenHouseDT_Manual.pdf /home/lab/Desktop/

echo "
url: http://localhost:8086
org: UiO

token: $token
# Uncomment the following line if you are using the infrastructure as demo
# and comment the GreenHouse bucket
bucket: GreenHouseDemo
# Uncomment the following line if you are using the infrastructure as greenhouse
# and comment the GreenHouseDemo bucket
#bucket: GreenHouse
" > sudo /home/lab/smol/config_local.yml

sudo chown -R lab: /home/lab/

# Add the csv file under /var/www/greentween.local/basic_data.csv to influxdb
sudo cp /var/www/greentween.local/basic_data.csv /home/lab/basic_data.csv
sudo chown lab: /home/lab/basic_data.csv

influx write --bucket GreenHouseDemo --org UiO --token $token -f /home/lab/basic_data.csv

rm /home/lab/basic_data.csv

ln -s /home/lab/smol /home/lab/Desktop/SimulationDriver

sudo chown -R lab: /home/lab/Desktop/

# Create the script for the execution
cd /home/lab/Desktop/
echo "
#!/bin/bash
cd /home/lab/smol/

# Check if the number of arguments is zero
if [ $# -eq 0 ]; then
  echo "Error: No arguments provided. Usage: $0 [start|stop]"
  exit 1
fi

# Check the value of the first argument
case $1 in
  "start")
    # Execute the start operation
    echo "Starting the process..."
    java -jar smol_scheduler.jar > /model/model.txt &
    ;;
  "stop")
    # Execute the stop operation
    echo "Stopping the process..."
    pkill -f smol_scheduler.jar
    ;;
  *)
    # Invalid argument
    echo "Error: Invalid argument. Usage: $0 [start|stop]"
    exit 1
    ;;
esac

exit 0" > /home/lab/Desktop/execute_simulation.sh

sudo chown lab: /home/lab/Desktop/execute_simulation.sh
sudo chmod +x /home/lab/Desktop/execute_simulation.sh

sudo cp /var/www/greentween.local/execution_mode.sh /home/lab/Desktop/change_parameters.sh
sudo chown lab: /home/lab/Desktop/change_parameters.sh

sudo snap install firefox
sudo reboot now