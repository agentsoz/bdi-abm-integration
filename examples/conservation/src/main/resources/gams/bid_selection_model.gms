$ontext

A model to select combinatorial bids.
Author: Md Sayed Iftekhar


$offtext

$if not set numPackages $abort 'no include file name for data file provided'
$if not set numBidders $abort 'no include file name for data file provided'


set
package /Malleefowl, Phascogale, Python, bid/
bidNum /1*%numPackages%/
bidders /1*%numBidders%/
target /mallNum, phasNum, pythNum/
targetdummy/1/
;

table target_table (targetdummy,target)
*$offlisting
$ondelim
$include "%target_table%"


$offdelim
*$onlisting


;;


$if not set csvInputFile $abort 'no input data file provided'

table Bid (bidNum, bidders,package)
*$offlisting
$ondelim
$include "%csvInputFile%"


$offdelim
*$onlisting


;


variable

z
zcost

;

positive variable

                mallee(bidNum, bidders)
                phas(bidNum, bidders)
                pyth(bidNum, bidders)
                ask(bidNum, bidders)
                malleeTot
                phasTot
                pythTot



;

Binary variables
                x(bidNum, bidders);

Equations
                Emallee(bidNum, bidders)
                Ephas(bidNum, bidders)
                Epyth(bidNum, bidders)
                EmalleeTot
                EphasTot
                EpythTot
                Eask
                Ecost
                Esox(bidders)

;


Emallee(bidNum, bidders).. mallee(bidNum, bidders)=e= Bid(bidNum, bidders, 'Malleefowl')*x(bidNum, bidders);
Ephas(bidNum, bidders).. phas(bidNum, bidders)=e= Bid(bidNum,bidders, 'Phascogale')*x(bidNum, bidders);
Epyth(bidNum, bidders).. pyth(bidNum, bidders)=e= Bid(bidNum, bidders,'Python')*x(bidNum, bidders);
Eask(bidNum, bidders).. ask(bidNum, bidders)=e= Bid(bidNum, bidders,'bid')*x(bidNum, bidders);

EmalleeTot.. malleeTot =e= sum(bidders, sum(bidNum, mallee(bidNum, bidders)));
EphasTot.. phasTot =e= sum(bidders, sum(bidNum, phas(bidNum, bidders)));
EpythTot.. pythTot =e= sum(bidders, sum(bidNum, pyth(bidNum, bidders)));

Esox(bidders).. sum(bidNum, x(bidNum, bidders)) =l= 1;

Ecost.. z =e= sum(bidders, sum (bidNum, ask(bidNum, bidders)));



model bidselection / Emallee,
                Ephas,
                Epyth,
                EmalleeTot,
                EphasTot,
                EpythTot,
                Eask,
                Esox,
                Ecost
                /;


malleeTot.lo =  target_table ('1','mallNum');
phasTot.lo = target_table ('1','phasNum');
pythTot.lo = target_table ('1','pythNum');

solve bidselection using mip minimizing z;


set select1 /bidder, bidnum, Malleefowl, Phascogale, Python, bid, win/;




parameter result(bidders, bidNum, select1);


result(bidders, bidNum, 'bidder')= bidders.val;
result(bidders, bidNum, 'bidnum')= bidNum.val;
result(bidders, bidNum, 'Malleefowl')=Bid(bidNum, bidders, 'Malleefowl');
result(bidders, bidNum, 'Phascogale')= Bid(bidNum,bidders, 'Phascogale');
result(bidders, bidNum, 'Python')= Bid(bidNum, bidders,'Python');
result(bidders, bidNum, 'bid')= Bid(bidNum, bidders,'bid');
result(bidders, bidNum, 'win')=x.l(bidNum, bidders);




display result;

$if not set csvOutputFile $abort 'no output data file name provided'

*$ontext

file bid_selection_result /%csvOutputFile%/;
put bid_selection_result;
bid_selection_result.pc = 5; bid_selection_result.pw=32767;


loop(bidNum,

looP(select1,
         PUT select1.TL;
);
PUT/;looP(bidders,
        PUT bidders.TL;
         looP(select1,
                 PUT result(bidders,bidNum,  select1)   :12:3;
          );
                 PUT/);
);


*$offtext



$ontext
file bid_selection_result /%csvOutputFile%/;
put bid_selection_result;
bid_selection_result.pc = 5; bid_selection_result.pw=32767;


loop(bidders,

looP(select1,
         PUT select1.TL;
);
PUT/;looP(bidNum,
        PUT bidNum.TL;
         looP(select1,
                 PUT result(bidNum, bidders, select1)   :12:3;
          );
                 PUT/);
);


$offtext
