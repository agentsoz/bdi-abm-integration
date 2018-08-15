INPUT=$1
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
echo Fix dodgy "Type" entries
sed -i '' 's/House,,/House,Residential,/g' Locations.csv
sed -i '' 's/Business district/Business District/g' Locations.csv
