SHP="SCS-Refuge-Centroids"

unzip $SHP.zip
INPUT=$SHP'/'*.shp
echo write each feature to text file
ogrinfo -al $INPUT > shp_file.txt
echo get coordinate for each instance
sed -n 's/POINT[[:space:]](*//p' shp_file.txt |  rev | cut -c 2- | rev > test.txt #|gdaltransform -s_srs EPSG:4326 -t_srs EPSG:32754 -output_xy > test.txt
echo remove non relevant entries
sed -i '' '/-/d' test.txt
echo add header
sed -i '' '1i\
,,xcoord ycoord\
' test.txt
echo add commas, convert to csv
sed -i '' 's/ /,/g' test.txt
mv test.txt test.csv
sed 's/^..//' < test.csv > test2.csv
echo convert orig. shp to csv
ogr2ogr -f "CSV" test1.csv $INPUT
sed 's/..$//' < test1.csv> test3.csv
echo merge two csvs
paste -d, test3.csv test2.csv > Refuges.csv
echo remove junk
rm shp_file.txt
rm test*
rm -r $SHP/
