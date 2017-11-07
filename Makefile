.PHONY: tubcommit

tubcommit:
	cd ../tub-rmit-collaboration ; cat emptyline.txt >> README.md
	cd ../tub-rmit-collaboration ; git commit -m "regression test" -a ; git push

