# write each feature to text file
ogrinfo -al Address_Points\ Drawing/Address_Points\ Drawing.shp > shp_file.txt
# get coordinate for each instance
sed -n 's/POINT[[:space:]](*//p' shp_file.txt | gdaltransform -s_srs EPSG:4326 -t_srs EPSG:32754 -output_xy > test.txt
# remove non relevant entries
sed -i '' '/-/d' test.txt
# add header
sed -i '' '1i\
xcoord ycoord\
' test.txt
# add commas, convert to csv
sed -i '' 's/ /,/g' test.txt
mv test.txt test.csv
# convert orig. shp to csv
ogr2ogr -f "CSV" test1.csv Address_Points\ Drawing/Address_Points\ Drawing.shp
# merge two csvs
paste -d, test1.csv test.csv > Locations.csv
#remove junk
rm shp_file.txt 
rm test.csv
rm test1.csv
