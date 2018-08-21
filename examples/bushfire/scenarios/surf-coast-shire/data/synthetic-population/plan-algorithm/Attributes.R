
options(stringsAsFactors = F)

read_numbers<- function(numbers_file)
{
  df<-read.csv(numbers_file,header = F,sep=',', stringsAsFactors = F,strip.white = T)
  NUMBERS<-vector()
  
  for (row in 1:nrow(df))
  {
    
    if (floor(row/2)!=row/2)#hacky FIX
    {
      Type<-df[row,1] 
    }
    else
    {
      number<-as.numeric(df[row,1])
      names(number)<-Type 
      NUMBERS<-c(NUMBERS,number)
    }
    
  }
  return(NUMBERS)
  
}

read_dependents<- function(dependents_file)
{
  df<-read.csv(dependents_file,header = F,sep=',', stringsAsFactors = F,strip.white = T)
  NUMBERS<-vector()
  
  for (row in 1:nrow(df))
  {
    
    if (floor(row/2)!=row/2)#hacky FIX
    {
      Type<-df[row,1] 
    }
    else
    {
      number<-as.numeric(df[row,1])
      names(number)<-Type 
      NUMBERS<-c(NUMBERS,number)
    }
    
  }
  return(NUMBERS)
  
}

read_thresholds<- function(thresholds_file)
{
  df<-read.csv(thresholds_file,header = F,sep=',', stringsAsFactors = F,strip.white = T)
  NUMBERS<-list()
  df<-as.matrix(df)
  for (row in 1:nrow(df))
  {
    
    if (row%%3==1)#hacky FIX
    {
      
      if(row>1){NUMBERS[[Type]]<-number}
      number<-vector()
      Type<-df[row] 
    }
    else
    {
      score<-as.numeric(df[row,2])
      names(score)<-df[row,1]
      number<-c(number,score)
    }
    
  }
  NUMBERS[[Type]]<-number
  return(NUMBERS)
  
}
read_locations_from_csv<-function(locations_csv_file)
{
  ## If Locations csv file is altered (i.e. a new source shp file), the names of the relevant columns might need to be changed here
  print("Reading locations from Locations.csv...")
  location_type_title = "Type"
  xcoord_title = "xcoord"
  ycoord_title = "ycoord"
  allocation_title="Count"
  address_title="EZI_ADDRES"
  locality_title="LOCALITY_N"
  base_node_id="GPO"
  
  #read csv
  locs<-read.csv(locations_csv_file,stringsAsFactors = F)
  #get only locations with positive allocations
  locs<-locs[locs[[allocation_title]]>0,]
  #reduce to necessary information
  locations<-locs[,c(location_type_title,allocation_title,xcoord_title,ycoord_title,address_title,locality_title)]
  # #distance matrix for all localities
  # 
  # d=as.matrix(dist(locations[locations[[location_type_title]]==base_node_id,3:4]))
  # locales<-locations[rownames(d),][[locality_title]]
  # rownames(d)<-locales
  # colnames(d)<-locales
  # 
  # 
  # LOCATIONS<-list(locations=locations,distances=d)
  return(locations)
}

write_attribute_plan<- function (plan_attributes,input_location,output_location)
{

base_plan<-read.delim(input_location, header = F,quote="")

broke<-strsplit(base_plan$V1,"person",fixed=F)
broke<-unlist(broke)
i=1
while (i<length(broke))
{
  if (grepl("id=",broke[i],fixed=T))
      {
        
        broke[i]=paste0(" <person ",broke[i])
        broke[i-1]=""
        
        if (length(plan_attributes)>0)
        { 
           
          agent<-plan_attributes[[1]]
          
          if (agent$dep==1) #HACKYFIX
          {
            home=strsplit(broke[i+2],"\"",fixed=T)
            depx=as.numeric(home[[1]][4])+(-1)*1*runif(1)
            depy=as.numeric(home[[1]][6])+(1)*1*runif(1)
            dep=paste0(depx,",",depy)
          }
          else
          {
            dep=""
          }
          broke=append(broke,"    </attributes>",i)
          broke=append(broke,"      <attribute name=\"InvacLocationPreference\" class=\"java.lang.String\">Anglesea Shops,777471,5742412</attribute>",i)
          broke=append(broke,"      <attribute name=\"EvacLocationPreference\" class=\"java.lang.String\">Torquay Foreshore,790771,5752462</attribute>",i)
          var=paste0("      <attribute name=\"FinalResponseThreshold\"   class=\"java.lang.Double\" >0.",agent$final_response,"</attribute>")
          broke=append(broke,var,i)
          var=paste0("      <attribute name=\"InitialResponseThreshold\" class=\"java.lang.Double\" >0.",agent$init_response,"</attribute>")
          broke=append(broke,var,i)
          var=paste0("      <attribute name=\"hasDependentsAtLocation\" class=\"java.lang.String\" >",dep,"</attribute>")
          broke=append(broke,var,i)
          var=paste0("      <attribute name=\"BDIAgentType\" class=\"java.lang.String\" >io.github.agentsoz.ees.agents.bushfire.",agent$subgroup,"</attribute>")
          broke=append(broke,var,i)
          broke=append(broke,"    <attributes>",i)
          plan_attributes[[1]]<-NULL
        }
      }
  else if (grepl("</",broke[i],fixed=T) & nchar(broke[i])==4 )
  {
    broke[i]=paste0(broke[i],"person>")
    broke[i+1]=""
  }
  
  i=i+1
}

plans<-file(output_location, open = "w+")
head<-broke
cat(head,file = plans, append=FALSE, sep = "\n")
close(plans)
}

set_attributes<- function(numbers,dependents,thresholds,stay)
{
  AGENTS<-list()
  for (subgroup in names(numbers))
  {
    for (i in 1:numbers[subgroup])
    {
    dep=0
    if (runif(1)<dependents[subgroup])
    {
      dep=1
      init_response<-sample(thresholds[[subgroup]][1]:3,1)
    }
    else
      {
        init_response<-sample(thresholds[[subgroup]][1]:thresholds[[subgroup]][2],1)
      }
    if (stay[subgroup])
      {
        final_response<-sample(init_response:thresholds[[subgroup]][2],1)
      }
      else
      {
        final_response<-init_response
      }
    sub<-strsplit(subgroup," ") #HACKY FIX
    sub<-unlist(sub)
    if (length(sub)>1)
    {
      sub<-paste0(sub[2],sub[1])  
    }
    
    AGENTS[[paste0(subgroup,"_",i)]]<-list(subgroup=sub,dep=dep,init_response=init_response,final_response=final_response)
    }
    
  }
 return (AGENTS) 
}

main<-function()
{
  
  args<-commandArgs(trailingOnly = T)
  # args<-c("typical-summer-weekday/numbers.csv",
  # "typical-summer-weekday/dependents.csv",
  # "typical-summer-weekday/thresholds.csv",
  # "typical-summer-weekday/stay.csv",
  # "typical-summer-weekday/plans.xml",
  # "typical-summer-weekday/test.xml")

  numbers<-read_numbers(numbers_file = args[1])
  dependents<-read_dependents(dependents_file = args[2])
  thresholds<-read_thresholds(thresholds_file = args[3])
  stay<-read_dependents(dependents_file = args[4])
  plan_attributes<-set_attributes(numbers,dependents,thresholds,stay)
  print("Appending BDI attributes to agents...")
  write_attribute_plan(plan_attributes,input_location = args[5],output_location = args[6])
}
main()