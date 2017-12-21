public class without sharing Q2C_DataCannon{

	public static void copyQuote(String quoteId, Date startDate, Date endDate, Integer numRecords, Decimal avgDisc)
	{
		List<Opportunity> oppsToInsert = new List<Opportunity>();
		List<SBQQ__Quote__c> qsToInsertA = new List<SBQQ__Quote__c>();
		List<SBQQ__Quote__c> qsToInsertB = new List<SBQQ__Quote__c>();

		String ss = '';
		List<Account> accts = [SELECT id, name FROM account LIMIT 100];
		Set<Id> qInsertIds = new Set<Id>();

		// Hold randomized values values
		Integer[] acctAssignments = new Integer[]{};
		Integer[] stageAssignments = new Integer[]{};
		Integer[] termAssignments = new Integer[]{};
		Double randomD = math.random();
		Integer rand100 = (randomD * 100).intValue();

		// IMPROVEMENT: Query Opportunity.StageName picklist values
		List<String> stageValues = new List<String>{'Prospecting','Qualification','Needs Analysis','Value Proposition','Id. Decision Makers','Perception Analysis','Proposal/Price Quote','Negotiation/Review','Closed Lost'};
		List<String> oppNames = new List<String>{'(Pilot)','(Tradeshow)','(Enterprise Deal)','(One Division)','(Two Divisions)','(Three Divisions)','(Global)', '(New Business)','(Partner Sale)'};
		List<String> liabilityCaps = new List<String>{'1x Payments','2x Payments', '3x Payments', 'Custom'};
		List<String> governingLaws = new List<String>{'State of California','State of Delaware','State of New York'};
		Integer stageCount = stageValues.size();

		// Randomize Assignment Values 
		// IMPROVEMENT: User control term
		// IMPROVEMENT: Enable Clustering of Accounts

		Integer totalMonths = startDate.monthsbetween(endDate);
		Double avgOppsMonth = numRecords/totalMonths;
		Date mutableDate = Date.newInstance(startDate.year(), startDate.month(), 15);
		Date now = Date.today();
		String stageValue;
		Integer stageNameAssignment;
		Integer acctCounter = 0;

		for(Integer i = 0;i<totalMonths;i++)
		{
		    for(Integer ii = 0;ii<avgOppsMonth;ii++)
		    {
		        // Limits past Opportunity to Won/Lost
		        // IMPROVEMENT: Allow users to set win/loss rate
		        stageNameAssignment = (randomD*stageCount).intValue();
		        
		        if(mutableDate < now)
		        {
		            if(randomD > 0.30){
		                stageValue = 'Closed Won';
		            }
		            else{
		                stageValue = 'Closed Lost';
		            }
		        }
		        else{
		            stageValue = stageValues.get(stageNameAssignment);
		        }
		        
		        Account aa = accts.get(acctCounter);
		        Opportunity opp = new Opportunity(Name = aa.name + ' ' + oppNames.get(stageNameAssignment), AccountId = aa.id, CloseDate = mutableDate, StageName = stageValue);
		        SBQQ__Quote__c qt = new SBQQ__Quote__c(SBQQ__StartDate__c = mutableDate, SBQQ__Primary__c = false, SBQQ__Account__c = aa.id);
		        
		        oppsToInsert.add(opp);
		        qsToInsertA.add(qt);
		        randomD = math.random();
		        if(acctCounter < 98)
		        {
		            acctCounter++;
		        }else
		        {
		            acctCounter = 0;
		        }
		    }
		    mutableDate = mutableDate.addMonths(1);
		}

		// Insert Opportunities & Quotes
		// NOTE: The Opportunity is not connected to the Opportunity. This must be accomplished w/a Batch Process

		Database.SaveResult[] srOpps = Database.Insert(oppsToInsert);
		Id oId;
		for(Integer i = 0;i<srOpps.size();i++)
		{
		    oId = srOpps[i].getId();
		    SBQQ__Quote__c qtt = qsToInsertA.get(i);
		    qtt.SBQQ__Opportunity2__c = oId;
		    qsToInsertB.add(qtt);
		}

		Database.SaveResult[] srQuotes = Database.insert(qsToInsertB);
		for(Integer i = 0;i<srOpps.size();i++)
		{
		    qInsertIds.add(srQuotes[i].getId());
		}

		//CREATE MASTER LINE LIST

		List<SBQQ__QuoteLine__c> masterLines = new List<SBQQ__QuoteLine__c>();

		SObjectType lineToken = Schema.getGlobalDescribe().get('SBQQ__QuoteLine__c');
		DescribeSObjectResult lineDef = lineToken.getDescribe();
		Map<String, SObjectField> lineFieldsMap = lineDef.fields.getMap();
		List<String> lineFieldList = new List<String>(lineFieldsMap.keySet());
		String lineQuery = 'SELECT ' + String.join(lineFieldList, ',') + ' FROM SBQQ__QuoteLine__c WHERE SBQQ__Quote__r.id =: quoteId';
		masterLines = Database.query(lineQuery);

		ID batchProcessID = Database.executeBatch(new qLineBlaster(masterLines, qInsertIds, avgDisc), 50);

	}	

}
