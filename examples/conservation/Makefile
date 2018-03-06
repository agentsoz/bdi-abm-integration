all: dist

dist:
	cd ../../ && mvn clean install -N
	cd ../../integrations/bdi-abm/ && mvn clean install
	cd ../../integrations/bdi-gams/ && mvn clean install
	cd ../../integrations/bdi-matsim/ && mvn clean install
	mvn clean install
