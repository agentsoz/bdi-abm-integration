.PHONY: tubcommit

QUICK=-Dmaven.test.skip -Dmaven.javadoc.skip -Dsource.skip -Dassembly.skipAssembly=true -DskipTests

tubcommit:
	cd ../tub-rmit-collaboration ; cat emptyline.txt >> README.md
	cd ../tub-rmit-collaboration ; git commit -m "regression test" -a ; git push

quick-matsim:
	cd ../matsim && make matsim-quick

quick: quick-matsim
	mvn clean install ${QUICK} --offline
	cd examples/bushfire ; mvn test -Dtest=MainCampbellsCreek01Test

normal: quick-matsim
	mvn clean install ${QUICK}
	cd integrations/abm-jack ; mvn clean install
	cd examples/bushfire-tutorial ; mvn clean install
	cd examples/bushfire-tutorial ; mvn test -Dmaven.test.redirectTestOutputToFile

