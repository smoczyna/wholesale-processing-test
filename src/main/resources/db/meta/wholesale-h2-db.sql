CREATE TABLE financialmarket (
    financialmarketid varchar(100), 
    fcccgsamapenddate varchar(100), 
    financialmarketmapenddate varchar(100), 
    glmarketlegalentityenddate varchar(100), 
    glmarketmaptype varchar(100), 
    glmarketenddate varchar(100), 
    alternatebookingtype varchar(100), 
    fcccgsamapeffectivedate varchar(100), 
    fcccgsanumber varchar(100), 
    financialmarketdefaultgeocode varchar(100), 
    financialmarketeffectivedate varchar(100), 
    financialmarketenddate varchar(100), 
    financialmarketmapeffectivedate varchar(100), 
    financialmarketname varchar(100), 
    financialmarkettypecode varchar(100), 
    gllegalentityid varchar(100), 
    glmarketdescription varchar(100), 
    glmarketeffectivedate varchar(100), 
    glmarketid varchar(100), 
    glmarketlegalentityeffectivedate varchar(100), 
    multiplemarketindicator varchar(100), 
    sidbid varchar(100));

CREATE TABLE wholesaleprice (
    productid BIGINT, 
    homesidbid varchar(100), 
    servesidbid varchar(100), 
    effectivedate varchar(100), 
    enddate varchar(100), 
    productdiscountpercent decimal, 
    productwholesaleprice decimal, 
    rateperiodclassificationid varchar(100));

CREATE TABLE financialeventcategory (
    productid BIGINT, 
    homesidequalsservingsidindicator varchar(100), 
    financialeventnormalsign varchar(100), 
    alternatebookingindicator varchar(100), 
    financialmarketid varchar(100), 
    interexchangecarriercode BIGINT, 
    adminfeereasoncode varchar(100), 
    bamsaffiliateindicator varchar(100), 
    billingaccrualindicator varchar(100), 
    billtypecode varchar(100), 
    cashpostindicator varchar(100), 
    cellularcarrierrslcode varchar(100), 
    companycode varchar(100), 
    contracttermsid BIGINT, 
    creditcardtypecode varchar(100), 
    debitcreditindicator varchar(100), 
    directindirectindicator varchar(100), 
    exceptioncode varchar(100), 
    financialcategory BIGINT, 
    financialcategorydescription varchar(100), 
    financialcategoryeffectivedate varchar(100), 
    financialcategoryenddate varchar(100), 
    financialeventcategorycode varchar(100), 
    financialeventcategorytype varchar(100), 
    financialeventdescription varchar(100), 
    financialeventdetails varchar(100), 
    financialeventeffectivedate varchar(100), 
    financialeventenddate varchar(100), 
    financialeventnumber BIGINT, 
    financialeventprocessingcode varchar(100), 
    financialgroupcode varchar(100), 
    financialmappingeffectivedate varchar(100), 
    financialmappingenddate varchar(100), 
    financialmarketsourcecode varchar(100), 
    foreignservedindicator varchar(100), 
    glaccountdescription varchar(100), 
    glaccountnumber varchar(100), 
    glcostcenternumber varchar(100), 
    gleffectivedate varchar(100), 
    glenddate varchar(100), 
    glfinancialaccounttypecode varchar(100), 
    gllocationid varchar(100), 
    glproductid varchar(100), 
    legalentityeqindicator varchar(100), 
    miscfinancialtransactionnumber BIGINT, 
    paymentmediacode varchar(100), 
    paymentsourcecode varchar(100), 
    pricebandtypecode varchar(100), 
    processinggroupnumber varchar(100), 
    statecode varchar(100), 
    taxjurisdictionlevel varchar(100), 
    taxtypecode varchar(100), 
    transactionitemtypecode varchar(100));

CREATE TABLE dataevent (
    productid BIGINT, 
    dataeventsubtype varchar(100), 
    billsectioncode varchar(100), 
    contentcomponenttype varchar(100), 
    contentreporttype varchar(100), 
    contentrevenuesharetype varchar(100), 
    dataeventtype varchar(100), 
    productbillfrequencycode varchar(100), 
    productbilllevelcode varchar(100));
