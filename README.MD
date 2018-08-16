## Predisposizione Consul

### Avvio 

avvio in modalita dev con binding per i client sulla 0.0.0.0

consul agent -dev -client=0.0.0.0

### Registrazione servizi


PUT http://13.90.89.12:8500/v1/agent/service/register

curl -XPUT -H"Content-Type: application/json" -d @../consul/registrazione_servizi/heroku.json  http://13.90.89.12:8500/v1/agent/service/register

curl -XPUT -H"Content-Type: application/json" -d @../consul/registrazione_servizi/bluemix.json  http://13.90.89.12:8500/v1/agent/service/register

curl -XPUT -H"Content-Type: application/json" -d @../consul/registrazione_servizi/openshift.json  http://13.90.89.12:8500/v1/agent/service/register

**Servizio Bluemix**

{
  "ID": "pdfservice",
  "Name": "pdfservice",
  "Tags": [
    "bluemix",
    "v1"
  ],
  "Address": "https://nomishapproxy.mybluemix.net/pdftotext",
  "Port": 443,
  "EnableTagOverride": false,
  "Check": {
    "DeregisterCriticalServiceAfter": "90m",
    "HTTP": "https://nomishapproxy.mybluemix.net/ping",
    "Interval": "5s"
  }
}

	

**Servizio Heroku**

	{
  "ID": "pdfservice1",
  "Name": "pdfservice",
  "Tags": [
    "heroku",
    "v1"
  ],
  "Address": "https://proxyarticolo.herokuapp.com/proxy/pdftotext",
  "Port": 443,
  "EnableTagOverride": false,
  "Check": {
    "DeregisterCriticalServiceAfter": "90m",
    "HTTP": "https://proxyarticolo.herokuapp.com/proxy/ping",
    "Interval": "5s"
  }
}
	
**Servizio OpenShift**
{
  "ID": "pdfservice2",
  "Name": "pdfservice",
  "Tags": [
    "openshift",
    "v1"
  ],
  "Address": "http://openshiftproxy-openshiftproxy11.7e14.starter-us-west-2.openshiftapps.com/pdftotext",
  "Port": 443,
  "EnableTagOverride": false,
  "Check": {
    "DeregisterCriticalServiceAfter": "90m",
    "HTTP": "http://openshiftproxy-openshiftproxy11.7e14.starter-us-west-2.openshiftapps.com/ping",
    "Interval": "5s"
  }
}


## utilizzo del proxy

Creaazione del JAR

	$ mvn clean compile assembly:single

eseguire il proxy con java -jar xxx.jar


## invocazione del servizio

http://localhost:5555/x/pdfservice

