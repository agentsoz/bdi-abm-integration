
SHP="SCS-Addresses-Subset"

unzip $SHP.zip
INPUT=$SHP'/'*.shp
echo write each feature to text file
ogrinfo -al $INPUT > shp_file.txt
echo get coordinate for each instance
sed -n 's/POINT[[:space:]](*//p' shp_file.txt | gdaltransform -s_srs EPSG:4326 -t_srs EPSG:32754 -output_xy > test.txt
echo remove non relevant entries
sed -i '' '/-/d' test.txt
echo add header
sed -i '' '1i\
xcoord ycoord\
' test.txt
echo add commas, convert to csv
sed -i '' 's/ /,/g' test.txt
mv test.txt test.csv
echo convert orig. shp to csv
ogr2ogr -f "CSV" test1.csv $INPUT
echo Append count column
awk -F "\"*,\"*" '{print $0fs",1"}' test1.csv>test2.csv
sed -i '' 's/Type,1/Type,Count/g' test2.csv
echo merge two csvs
paste -d, test2.csv test.csv > Locations.csv
echo remove junk
rm shp_file.txt
rm test*
rm -r $SHP/
echo Fix dodgy "Type" entries
#set all residentials to 0
sed -i '' 's/House,Residential,1/House,Residential,0/g' Locations.csv
#Select relevant locales
awk  'BEGIN {OFS=FS=","} {if ($12=="ANGLESEA"&&$17=="Residential") $18=1;print}' Locations.csv> test3.csv
awk 'BEGIN {OFS=FS=","} {if ($12=="AIREYS INLET"&&$17=="Residential") $18=1;print}' test3.csv > test4.csv
awk 'BEGIN {OFS=FS=","} {if ($12=="FAIRHAVEN"&&$17=="Residential") $18=1;print}' test4.csv> test3.csv
awk 'BEGIN {OFS=FS=","} {if ($12=="JAN JUC"&&$17=="Residential") $18=1;print}' test3.csv> test4.csv
awk 'BEGIN {OFS=FS=","} {if ($12=="TORQUAY"&&$17=="Residential") $18=1;print}' test4.csv> test3.csv
awk 'BEGIN {OFS=FS=","} {if ($12=="LORNE"&&$17=="Residential") $18=1;print}' test3.csv> Locations.csv
sed -i '' 's/House,,1/House,Residential,0/g' Locations.csv
sed -i '' 's/Business district/Business District/g' Locations.csv
echo Add "Out of Region" locations
echo '"999998","0","0","0",,,,,,,,APOLLO BAY,,,,,Out of Region,20,726428.3187297015,5706161.232068798' >> Locations.csv
echo '"999999","0","0","0",,,,,,,,MELBOURNE,,,,,Out of Region,80,841924.6,5808324.9' >> Locations.csv
echo appending GPO Locations
cut -d',' -f12 Locations.csv |sort|uniq|tail -n +2|while read -r line; do grep ",$line," BaseLocations.csv >> Locations.csv; done
rm test*
