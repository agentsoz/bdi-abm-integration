.PHONY: tubcommit

QUICK=-Dmaven.test.skip -Dmaven.javadoc.skip -Dsource.skip -Dassembly.skipAssembly=true -DskipTests --offline

tubcommit:
	cd ../tub-rmit-collaboration ; cat emptyline.txt >> README.md
	cd ../tub-rmit-collaboration ; git commit -m "regression test" -a ; git push

quick:
	cd ../matsim ; make matsim-quick
	mvn clean install ${QUICK}
	cd examples/bushfire ; mvn test -Dtest=MainCampbellsCreek01Test

normal:
	cd ../matsim ; make matsim-quick
	mvn clean install ${QUICK}
	cd integrations/abm-jack ; mvn clean install
	cd examples/bushfire-tutorial ; mvn clean install
# mvn test -Dmaven.test.redirectTestOutputToFile 
	cd examples/bushfire-tutorial ; mvn test -Dmaven.test.redirectTestOutputToFile
