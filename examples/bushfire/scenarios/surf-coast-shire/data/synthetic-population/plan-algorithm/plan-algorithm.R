
options(stringsAsFactors = F)

derive_start_time <- function (n_activity,durations)
{
  activity_name<-rownames(n_activity)[1]
  s_activity=matrix(0, nrow = dim(n_activity)[1], ncol = dim(n_activity)[2], dimnames=dimnames(n_activity)) # matrix to store start times
  t_work<-n_activity # temp copy of work matrix
  #print("t_work"); print(t_work)
  typical_duration<-durations[activity_name]
  block_size<-24/length(n_activity[activity_name,])
  for(h in 1:length(n_activity[activity_name,])) {
    h_work<-t_work[activity_name,h] # find out how many started work this hour
    #cat("h_work", h_work, "\n")
    s_activity[activity_name,h]<-h_work # save the start times for this hour
    #print("s_activity"); print(s_activity)
    block<-typical_duration/block_size # towards future bins to impact 
    block<-min(length(n_activity[activity_name,])-h+1, block) # towards future bins to impact
    bins<-h:(h+block-1) # ids of bins impacted
    #cat("bins impacted:", bins, "\n")
    t_work[activity_name, bins]<-t_work[activity_name, bins]-h_work # remove those who started work this hour
    t_work[t_work<0]<-0 # don't go below zero
    #print("t_work"); print(t_work)
  }
  return(s_activity)
}

prob_matrix<-function (n_activities,durations,repeating,type)
{
  
  if (sum(colSums(n_activities)-100)!=0)
  {
    print("ERROR: one or more columns in distribution table do not sum to 100%")
    print(colSums(n_activities))
    break
  }  
  
  s_times<-matrix(0, nrow=nrow(n_activities), ncol=ncol(n_activities), dimnames=dimnames(n_activities))
  for(activity in rownames(n_activities)) {
    n_activity<-matrix(n_activities[activity,], nrow=1, ncol=length(n_activities[activity,]), dimnames=list(activity))
    s_activity<-derive_start_time(n_activity,durations)
    s_times[activity,]<-s_activity
  }
  start<-s_times/100
  shift<-matrix(colSums(start),nrow(n_activities),12,byrow=T)
  saveshift<-shift
  rept<-(repeating-1)*-1 #invert repeat vector
  if(sum(rowSums(rept*start)>1))
  {
    problem<-which(rowSums(rept*start)>1)
    print(paste0("ERROR: non-repeatable activity ",names(problem)," cannot be completed by ",rowSums(rept*start)[problem]*100,"% of the ",type," population. Try increasing duration time for activity, reducing overall distribution of activity, or making activity repeatable."))
    break
  }
  
  dur<-durations
  #nonrepeat correction
  for (i in 1:nrow(n_activities))
  {
    if (rept[i]>0)
    {
      len=(rept*dur)[i]
      for (g in (1+len):12)
      {
        
        subj=shift[i,g]-sum(start[i,1:(g-len)])#sum(start[i,1:(g-len)])
        cont=(shift[i,g]-start[i,g])/(1-(start[i,g]/(subj)))
        shift[,g]=cont
        shift[i,g]=subj
      }
    }
    
  }
  prob=start/shift
  
  # p_resident<-1-(1-p_resident)^(1/p) #(for timesteps!=2 trying to spread prob according to timestep- a bit dodgy)
  cum_prob<-apply(prob,2,cumsum) #cumulative sums for each column
  return(cum_prob)
}

plan_times<-function (number_of_agents,probability_matrix,durations,repeating)
{  
  dur<-as.vector(durations)/2
  rept<-(repeating-1)*-1
  N<-number_of_agents
  n<-ncol(probability_matrix)
  nAct<-nrow(probability_matrix)
  PLANS<-list()
  AGENTS<-list()
  for (h in 1:N)
    
  {
    
    roll<-runif(n) #roll for each timestep 
    
    #activity is allocated at each timestep
    agent<-apply(probability_matrix,1,function(x){roll<x})  
    agent<-apply(agent,1,cumsum) 
    agent[agent>1]=0
    
    agent<-dur*agent #insert durations into matrix
    z=rep(0,nAct) #blank vector
    s=0  #block flag
    r=z  #repeat flag
    
    #cycle through to see which potential activities actually occur
    for (j in 1:n)
    {
      if(s>0)
      {
        agent[,j]=agent[,j-1] #block timesteps following activity.
        s=s-1
      }
      else #at this point, the timestep changes to it's activity
      {
        if (sum(r*agent[,j])>0) #check if unrepeatable activity has already occurred
        {
          pass=r
          while(sum(pass*agent[,j])>0)
          {
            
            agent[,j]<-dur*(abs(cumsum(probability_matrix[,j]>runif(1))-1)<1)#reroll
            pass=agent[,j]*rept #check repeat again
            
          }
          
        }
        
        s=sum(agent[,j])-1 # if a new activity is activated, set its       block duration
        r=r+agent[,j]*rept #check repeat
        
        
      } 
    }
    agent=agent/dur
    AGENTS[[h]]<-agent
    names<-row.names(agent)
    re=agent
    for (i in 2:12)
    {
      re[,i]=agent[,i]-agent[,i-1]
    }
    re[re<1]=0
    re=colSums(re*(row(re)))
    activity=names[re]
    re=as.matrix(re)
    start_time=re*(2*row(re)-1)*100/re
    start_time=start_time[complete.cases(start_time*0),,drop=FALSE]
    start_time=as.integer(start_time)
    start_time=start_time+c(-99,sample(c(-99:-41,0:59),length(start_time)-1))
    person=rep(h,length(start_time))
    plan=data.frame(cbind(person,activity,start_time))
    plan[]<-lapply(plan,as.character)
    
    ##Ensure resident starts and ends at home
    header<-plan[1,]
    header[2]<-"home"
    header[3]<-"0000"
    if (plan[1,2]=="home")
    {
      plan[1,3]=0
    } else
    {
      plan<-rbind(header,plan)
    }
    
    footer<-header
    footer[3]<-"2359"
    if (tail(plan,1)[2]!="home")
    {
      plan<-rbind(plan,footer)
    }
    
    ##add zero characters if necessary 
    for (yy in 1:nrow(plan))
    {
      lenn<-4-nchar(plan[yy,3])
      if(lenn>0)
        
      {
        for (yyy in 1:lenn)
        {
          plan[yy,3]<-paste0("0",plan[yy,3])
        }
      }
      plan[yy,3]<-paste0(substr(plan[yy,3],0,nchar(plan[yy,3])-2),":",substr(plan[yy,3],nchar(plan[yy,3])-1,nchar(plan[yy,3])),":00")
    }
    PLANS[[h]]<-plan
  }
  Agents<-Reduce('+',AGENTS)
  Agents<-100*Agents/number_of_agents
  output<-list(Agents=Agents,Plans=PLANS)
  return(output)
}

location_map<-function (locations_from_csv,location_names,subgroup)
{ 
  ##Get Location Coordinates
  location_type_title<-names(locations_from_csv)[1]
  LOCATIONS<-list()
  for (activity in names(location_names))
  {
    LOCATIONS[[activity]]<-locations_from_csv[locations_from_csv[[location_type_title]] %in% location_names[[activity]],]
    if (length(unique(LOCATIONS[[activity]][[location_type_title]]))!=length(location_names[[activity]]))
    {
      print(paste0("ERROR: activity '",activity,"' for type '",subgroup,"' has been assigned locations that do not exist in the locations data. Please change the location map. Possible locations are:"))
      show(levels(locations_from_csv[[location_type_title]]))    
      return()
    }
  }  
  
  return(LOCATIONS)
}

allocation_checker<-function(activity_locations,number_of_agents,subgroup)
{
    # for (activity in names(activity_locations))
    activity="home" ##allocations only in place for home location at the moment
    {
      if (sum(activity_locations[[activity]][,2])<number_of_agents)
      {
        print(paste0("WARNING: number of ",subgroup,"s exeeds total allocation for the corresponding ",activity," location map. Adjusting each mapped allocation to ",number_of_agents,". To avoid this, change the allocations in the locations SHP file and run again."))
        activity_locations[[activity]][,2]=number_of_agents
      }
      
    }
  return(activity_locations)
}

plan_locations<-function (plan_times,activity_locations,locality_distances)
{
  PLANS<-plan_times  
  for (h in 1:length(PLANS))
  {
    plan<-PLANS[[h]]  
    
    #choose home location
    valid<-activity_locations[[1]][activity_locations[[1]][2]>0,]
    choice<-sample(nrow(valid),1)
    home<-valid[choice,]
    #change allocation for that home
    activity_locations[[1]][rownames(home),][2]=activity_locations[[1]][rownames(home),][2]-1
    
    
    #add home to plan
    plan$ycoord=unlist(plan$ycoord)
    plan$xcoord=matrix(home[3],nrow(plan))
    plan$ycoord=matrix(home[4],nrow(plan))
    plan$xcoord=unlist(plan$xcoord)
    
    
    
    ##find appropriate location for other activities
    #home[]<-lapply(home,as.character)
    locality<-unlist(as.vector(home[6]))
    
   
    
    for (i in 1:nrow(plan))
    {
      activity<-plan[i,2]
      if (activity!="home")
      {
        # 
        # ## OLD METHOD (using probability based on 1/distance^2)
        # #find Euclidean distance between home and potential destinations  
        # distances=activity_locations[[activity]][,3:4]-unlist(matrix(home[2:3],nrow(activity_locations[[activity]]),2,byrow = T))
        # distances=distances^2
        # distances=sqrt(rowSums(distances))
        # distances=distances[distances!=0] #remove home from activity list if it is there
        # #choose based on inverse square law ##ASSUMPTION
        # 
        # place=sample(distances,1,prob = 1/(distances^2))
        distances<-locality_distances[locality,names(locality_distances[locality,]) %in% unique(activity_locations[[activity]][[6]])]
        
        #set relative likelihood of staying in locality (ASSUMPTION) -- this gives approx 2/3 chance that will stay in locale
        if(locality %in% names(distances)) {distances[locality]<-1000/(2*sum (1000/distances[names(distances)!=locality]))}
        
        choose_locality=sample(distances,1,prob = 1000/(distances))
        locality=names(choose_locality)
        choose=sample(nrow(activity_locations[[activity]][activity_locations[[activity]][[6]]==locality,]),1)
        place=activity_locations[[activity]][activity_locations[[activity]][[6]]==locality,][choose,]
        plan[i,4:5]=place[3:4]
      }
    }
    PLANS[[h]]<-plan
    
  }
  pass_plans<-list(PLANS=PLANS,updated_activity_locations=rownames(activity_locations[[1]][activity_locations[[1]][[2]]==0,]))
  return(pass_plans)
}

type_plan<-function (n_activities,number_of_agents,locations_from_csv,location_names,type)
{
  if (number_of_agents==0)
  {
    return()
  }
  else
    print(paste0("Forming plans for ",number_of_agents," ",type,"s..."))
  
  #switching durations and repeating off for now (making them uniform)
  durations<-rep(2,nrow(n_activities))
  names(durations)<-rownames(n_activities)
  
  #WORK EXCEPTION
  if ("work" %in% rownames(n_activities))
  {
    durations["work"]<-4
  }
  repeating<-rep(1,nrow(n_activities))
  names(repeating)<-rownames(n_activities)
  
  # n_activities<-DISTRIBUTIONS[[Type]]
  # number_of_agents<-NUMBER[Type]
  # location_names<-LOCATIONS[[Type]]
  # type<-Type
  # 
  probability_matrix<-prob_matrix(n_activities = n_activities,durations = durations,repeating = repeating,type = type)
  plan_times_type<-plan_times(number_of_agents = number_of_agents,probability_matrix = probability_matrix,durations = durations,repeating)
  

  activity_locations<-location_map(locations_from_csv =locations_from_csv[[1]],location_names=location_names,subgroup=type)
  activity_locations<-allocation_checker(activity_locations,number_of_agents,subgroup = type)
  PLANS<-plan_locations(plan_times = plan_times_type$Plans,activity_locations =activity_locations,locality_distances=locations_from_csv[[2]])  
  output<-list(Agents=plan_times_type$Agents,Plans=PLANS$PLANS,updated_activity_locations=PLANS$updated_activity_locations)
  
  return(output)
}

write_log<-function (AGENTS,DISTRIBUTIONS)
{
  print("Writing to log file...")
  log<-file("log.txt", open = "w+")
  cat("DIFFERENCE TABLES:\nThese tables show the percentage error from the inputted distribuations at each 2 hour time block (ignore for small populations).",
      file = log, append=FALSE, sep = "\n")
  for (Type in names(AGENTS))
  {
    
    diff<-round(AGENTS[[Type]]-DISTRIBUTIONS[[Type]],2)
    cat(paste0("\n",Type),file = log, append=FALSE, sep = "\n")
    write.table(diff, file=log, row.names=TRUE,sep="\t", col.names=NA)
    
    
  }
  
  close(log) 
}

write_xml<-function (PLANS,output_location)
{
  print("Writing plans to XML file...")
  plans<-file(output_location, open = "w+")
  head<-'<?xml version="1.0" encoding="utf-8"?>
  <!DOCTYPE population SYSTEM "http://www.matsim.org/files/dtd/population_v6.dtd">
  <population>
  <!-- ====================================================================== -->'
  cat(head,file = plans, append=FALSE, sep = "\n")
  
  ct=0
  for (Type in names(PLANS))
  {
    type<-gsub(" ","_",Type)
    type<-gsub("-","_",type)
    for (i in 1:length(PLANS[[Type]]))
    {
      plan<-as.data.frame(PLANS[[Type]][i])
       person<-paste0('  <person id= "',ct,'" >\n    <plan selected="yes">\n      ')
      
      for (j in 1:nrow(plan))
      {
        if (j<nrow(plan))
        {
          person<-paste0(person,'<activity type="',plan[j,2],'" x="',plan[j,4],'" y="',plan[j,5],'" end_time="',plan[j+1,3],'" />\n      <leg mode="car" />\n      ')
          
        }
        else
        {
          if (j==1)
          {
            person<-paste0(person,'<activity type="',plan[j,2],'" x="',plan[j,4],'" y="',plan[j,5],'" end_time="06:00:00" />\n      <leg mode="car" />\n      ')
          }
          person<-paste0(person,'<activity type="',plan[j,2],'" x="',plan[j,4],'" y="',plan[j,5],'" />\n    </plan>\n  </person>\n  ')
          
        }
      }
      cat(person, file = plans, append=FALSE, sep = "\n")
      ct=ct+1
    }
  }
  foot<-'<!-- ====================================================================== -->
  
  </population>'
  cat(foot, file = plans, append=FALSE, sep = "\n")
  close(plans) 
}

read_distributions<-function(distributions_file)
{
  df<-read.csv(distributions_file,header = F,sep=',',stringsAsFactors = F,strip.white = T)
  DISTRIBUTIONS<-list()
  activities=vector()
  # NUMBER<-vector()
  # LOCATIONS<-list()
  # locations<-list()
  count=0
  for (row in 1:nrow(df))
  {
    
    #non-numeric rows:
    if (anyNA(df[row,])==T)
    {
      
      first<-as.character(df[row,][1])
      
      {
        if (count>0) #first wrtie in the finished previous one
        {
          DISTRIBUTIONS[[Type]]<-activities
          colnames(DISTRIBUTIONS[[Type]])<-c("01:00","03:00","05:00","07:00",
                                             "09:00","11:00","13:00","15:00",
                                             "17:00","19:00","21:00","23:00")
          # LOCATIONS[[Type]]<-locations
          activities<-vector()
          # locations<-list()
        }
        #now refresh
        Type=first
        # NUMBER=cbind(NUMBER,as.integer(df[row,][2]))
        count=count+1
      }
    }
    else #write the numeric vectors in according to activity type
    {
      Activity=as.character(df[row,][1])
      dist=as.vector(df[row,][2:length(df[row,])])
      # rownames(dist)=Activity
      # dist[,]<-sapply(dist[,],as.numeric)
      dist<-apply(dist,2,as.numeric)
      activities=rbind(activities,dist)
      rownames(activities)[nrow(activities)]<-Activity
    }
  }
  # NOT USED CURRENTLY as duration and repeating variation is switched off
  ##**** 3. Set the duration of each activity, for each type (MUST BE A MULTIPLE OF 2):                               
  # DURATIONS<-matrix(c(
  #   #RESIDENT 
  #   c(2,8,2,2,2),
  #   #PT RESIDENT
  #   c(2,8,2,2,2),
  #   #REG. VISITOR
  #   c(2,8,2,2,2),
  #   #O'N VISITOR
  #   c(8,8,2,2,2),
  #   #DAY VISITOR  
  #   c(2,8,4,2,2)),      nrow =5,byrow = T)
  # rownames(DURATIONS)<-c("Resident","PT Resident","Regular Visitor","Overnight Visitor","Day Visitor")
  # colnames(DURATIONS)<-rownames(DISTRIBUTIONS$Resident)
  # 
  # #**** 4. Set the repeating behaviour for each activity, for each type (MUST HAVE AT LEAST ONE REPEATING ACTIVITY PER TYPE):         
  # REPEATING<-matrix(c(
  #   #RESIDENT 
  #   c(1,0,1,1,1),
  #   #PT RESIDENT
  #   c(1,0,0,0,0),
  #   #REG. VISITOR
  #   c(1,0,1,1,1),
  #   #O'N VISITOR
  #   c(1,0,1,1,1),
  #   #DAY VISITOR  
  #   c(1,0,0,1,1)),      nrow =5,byrow = T)
  # rownames(REPEATING)<-c("Resident","PT Resident","Regular Visitor","Overnight Visitor","Day Visitor")
  # colnames(REPEATING)<-rownames(DISTRIBUTIONS$Resident)
  DISTRIBUTIONS[[Type]]=activities
  colnames(DISTRIBUTIONS[[Type]])<-c("01:00","03:00","05:00","07:00",
                                     "09:00","11:00","13:00","15:00",
                                     "17:00","19:00","21:00","23:00")
  return(DISTRIBUTIONS)
}

read_locations<-function(s_file,types)
{
  df<-read.csv(s_file,header = F,fill=T,sep=',',col.names = (1:20), stringsAsFactors = F,strip.white = T)
  
  
  LOCATIONS<-list()
  locations<-list()
  count=0
  for (row in 1:nrow(df))
  {
    #identify headers:
    if (df[row,1] %in% types)
    {
      
      if (count>0) #first wrtie in the finished previous one
      {
        
        LOCATIONS[[Type]]<-locations
        locations<-list()
      }
      #now refresh
      Type=df[row,1]
      # NUMBER=cbind(NUMBER,as.integer(df[row,][2]))
      count=count+1
      
    }
    else #write the location vectors in according to activity type
    {
      locs<-df[row,]
      locs<-locs[locs!=""]
      locs<-locs[!is.na(locs)]
      if(length(locs)<2)
      {
        print(paste0("ERROR: activity '",locs[1],"' for type '",Type,"' has not been assigned any locations in the locations map."))
        return()
      }
      
      Activity<-locs[1]
      locations[[Activity]]<-locs[2:length(locs)]
    }
  }
  
  LOCATIONS[[Type]]=locations
  
  return(LOCATIONS)
}

read_numbers<- function(numbers_file,types)
{
  df<-read.csv(numbers_file,header = F,sep=',', stringsAsFactors = F,strip.white = T)
  NUMBERS<-vector()
  
  for (row in 1:nrow(df))
  {
    
    if (df[row,1] %in% types)
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
  #distance matrix for all localities
   
  d=as.matrix(dist(locations[locations[[location_type_title]]==base_node_id,3:4]))
  locales<-locations[rownames(d),][[locality_title]]
  rownames(d)<-locales
  colnames(d)<-locales

  
  LOCATIONS<-list(locations=locations,distances=d)
  return(LOCATIONS)
}

inputs<-function (distributions_file,locations_file,numbers_file)
{
  DISTRIBUTIONS<-read_distributions(distributions_file)
  
  types<-names(DISTRIBUTIONS)
  LOCATIONS<-read_locations(locations_file,types)
  NUMBERS<-read_numbers(numbers_file,types)
  
  INPUT<-list(numbers=NUMBERS,distributions=DISTRIBUTIONS,locations=LOCATIONS)
  return(INPUT)  
}

main<-function ()
{
  # args<-commandArgs(trailingOnly = T)
  args<-c("typical-summer-weekday/distributions.csv",
  "typical-summer-weekday/location_maps.csv",
  "typical-summer-weekday/numbers.csv",
  "Locations.csv",
  "typical-summer-weekday/plans.xml")

  input<-inputs(distributions_file = args[1],locations_file = args[2],numbers_file = args[3])  
  locations_csv<-read_locations_from_csv(args[4])
  PLANS<-list()
  AGENTS<-list()
   
  for (Type in names(input$numbers))
  {
    run<-type_plan(n_activities = input$distributions[[Type]],number_of_agents =input$numbers[Type],locations_from_csv = locations_csv,location_names = input$locations[[Type]],type = Type)
    PLANS[[Type]]<-run$Plans
    AGENTS[[Type]]<-run$Agents
    locations_csv$locations[run$updated_activity_locations,][[2]]<- locations_csv$locations[run$updated_activity_locations,][[2]]-1
    
  }
  
  
  #write plan.xml (to working directory)
  write_xml(PLANS = PLANS,output_location = args[5])
  
  write_log(AGENTS = AGENTS,DISTRIBUTIONS = input$distributions)
  
  print("Finished.")
}
main()