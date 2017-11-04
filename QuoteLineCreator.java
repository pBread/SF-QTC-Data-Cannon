global class qLineBlaster implements Database.batchable<sObject>{
    global final Set<Id> qIds;
    global final Decimal aDisc;
    global final List<SBQQ__QuoteLine__c> mLns;
    global final String query;
    
    global qLineBlaster(List<SBQQ__QuoteLine__c> masterLines, Set<Id> quoteIds, Decimal avgDisc)
    {
        mLns = masterLines; qIds = quoteIds; aDisc = avgDisc;
        query = 'SELECT id FROM SBQQ__Quote__c WHERE id IN: qIds';
    }
    
    global Database.QueryLocator start(Database.BatchableContext info)
    {        
        return Database.getQueryLocator(query);
    }
    
    global void execute(Database.BatchableContext info, List<SBQQ__Quote__c> scope)
    {
        SBQQ.TriggerControl.disable();
        List<SBQQ__QuoteLine__c> linesToInsert = new List<SBQQ__QuoteLine__c>();
        Integer i = 0;
        Integer aDiscMutable;
        for(SBQQ__Quote__c s : scope)
        {
            List<SBQQ__QuoteLine__c> clonedLines = mLns.deepClone(false, false, false);
            for(SBQQ__QuoteLine__c line : clonedLines)
            {
                system.debug('quote line for loop ' + i);
                line.SBQQ__Quote__c = s.id;
                line.SBQQ__Discount__c = aDisc;
                linesToInsert.add(line);
            }
            
        }
        Database.insert(linesToInsert);
    }
    
    global void finish(Database.BatchableContext info)
    {
        SBQQ.TriggerControl.enable();
        system.debug('finish');
    }
    
}
