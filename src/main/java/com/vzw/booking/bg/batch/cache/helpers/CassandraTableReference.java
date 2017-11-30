
/**
 * 
 */
package com.vzw.booking.bg.batch.cache.helpers;

import com.vzw.booking.bg.batch.domain.casandra.DataEvent;
import com.vzw.booking.bg.batch.domain.casandra.FinancialEventCategory;
import com.vzw.booking.bg.batch.domain.casandra.FinancialMarket;
import com.vzw.booking.bg.batch.domain.casandra.WholesalePrice;

/**
 * @author torelfa
 *
 */
public class CassandraTableReference {

	/**
	 * Constructor
	 */
	private CassandraTableReference() {
		throw new IllegalStateException("CassandraTableReference: Reference Data");
	}

	/**
	 * 
	 */
	public static final String CACHE_ITEM_KEY_SEPARATOR="-";
	
	/**
	 * 
	 */
	public static final String CACHE_ITEM_BILLED_FINANCIAL_EVENT_CATEGORY="BILLEDFINANCIALEVENTCATEGORY";
	/**
	 * 
	 */
	public static final String CACHE_ITEM_UNBILLED_FINANCIAL_EVENT_CATEGORY="UNBILLEDFINANCIALEVENTCATEGORY";
	/**
	 * 
	 */
	public static final String CACHE_ITEM_FINANCIAL_MARKET="FINANCIALMARKET";
	/**
	 * 
	 */
	public static final String CACHE_ITEM_DATA_EVENT="DATAEVENT";
	/**
	 * 
	 */
	public static final String CACHE_ITEM_WHOLESALE_PRICE="WHOLESALEPRICE";
	/**
	 * 
	 */
	public static final char DEFAULT_CASSANDRA_HIDDEN_ECHO_CHARACTER='*';
	
	/**
	 * Index Helper for Billed FinancialEventCategory Data Object
	 * @param c
	 * @return
	 */
	public static final String UnBilledFinancialEventCategoryIndexHelper(FinancialEventCategory c) {
		return ""  + c.getProductid() + CACHE_ITEM_KEY_SEPARATOR + c.getHomesidequalsservingsidindicator() + 
				CACHE_ITEM_KEY_SEPARATOR + c.getAlternatebookingindicator() + CACHE_ITEM_KEY_SEPARATOR + c.getInterexchangecarriercode();
	}
	
	/**
	 * Index Helper for Billed FinancialEventCategory Data Object
	 * @param productId
	 * @param homeSidEqualsServingsIdIndicator
	 * @param alternateBookingIndicator
	 * @param interexchangeCarrierCode
	 * @return
	 */
	public static final String UnBilledFinancialEventCategoryIndexHelper(Integer productId, String homeSidEqualsServingsIdIndicator, String alternateBookingIndicator, Integer interexchangeCarrierCode) {
		return ""  + productId + CACHE_ITEM_KEY_SEPARATOR + homeSidEqualsServingsIdIndicator + 
				CACHE_ITEM_KEY_SEPARATOR + alternateBookingIndicator + CACHE_ITEM_KEY_SEPARATOR + interexchangeCarrierCode;
	}
	
	/**
	 * Index Helper for UnBilled FinancialEventCategory Data Object
	 * @param c
	 * @return
	 */
	public static final String BilledFinancialEventCategoryIndexHelper(FinancialEventCategory c) {
		return ""  + c.getProductid() + CACHE_ITEM_KEY_SEPARATOR + c.getHomesidequalsservingsidindicator() + 
				CACHE_ITEM_KEY_SEPARATOR + c.getAlternatebookingindicator() + CACHE_ITEM_KEY_SEPARATOR + c.getInterexchangecarriercode() +
				CACHE_ITEM_KEY_SEPARATOR + c.getFinancialeventnormalsign();
	}
	
	/**
	 * Index Helper for UnBilled FinancialEventCategory Data Object
	 * @param productId
	 * @param homeSidEqualsServingsIdIndicator
	 * @param alternateBookingIndicator
	 * @param interexchangeCarrierCode
	 * @param financialEventNormalSign
	 * @return
	 */
	public static final String BilledFinancialEventCategoryIndexHelper(Integer productId, String homeSidEqualsServingsIdIndicator, String alternateBookingIndicator, Integer interexchangeCarrierCode, String financialEventNormalSign) {
		return ""  + productId + CACHE_ITEM_KEY_SEPARATOR + homeSidEqualsServingsIdIndicator + 
				CACHE_ITEM_KEY_SEPARATOR + alternateBookingIndicator + CACHE_ITEM_KEY_SEPARATOR + interexchangeCarrierCode +
				CACHE_ITEM_KEY_SEPARATOR + financialEventNormalSign;
	}
	
	/**
	 * Index Helper for FinancialMaket Data Object
	 * @param c
	 * @return
	 */
	public static final String FinancialMaketIndexHelper(FinancialMarket c) {
		return "" + c.getFinancialmarketid();
	}
	
	/**
	 * Index Helper for FinancialMaket Data Object
	 * @param financialMarketId
	 * @return
	 */
	public static final String FinancialMaketIndexHelper(String financialMarketId) {
		return "" + financialMarketId;
	}
	
	/**
	 * Index Helper for DataEvent Data Object
	 * @param c
	 * @return
	 */
	public static final String DataEventIndexHelper(DataEvent c) {
		return "" + c.getProductid();
	}
	
	/**
	 * Index Helper for DataEvent Data Object
	 * @param productId
	 * @return
	 */
	public static final String DataEventIndexHelper(Integer productId) {
		return "" + productId;
	}
	
	/**
	 * Index Helper for WholesalePrice Data Object
	 * @param c
	 * @return
	 */
	public static final String WholesalePriceIndexHelper(WholesalePrice c) {
		return "" + c.getProductid() + CACHE_ITEM_KEY_SEPARATOR + c.getHomesidbid();
	}
	
	/**
	 * Index Helper for WholesalePrice Data Object
	 * @param productId
	 * @param homeSidbId
	 * @return
	 */
	public static final String WholesalePriceIndexHelper(Integer productId, String homeSidbId) {
		return "" + productId + CACHE_ITEM_KEY_SEPARATOR + homeSidbId;
	}
	
	/**
	 * Hide any not space character with echo one
	 * @param original String to be hidden
	 * @param echoChar echo character for replacement
	 * @return echoed string
	 */
	public static final String stringEchoing(String original, char echoChar) {
		if (original==null || original.trim().isEmpty()) {
			return original;
		}
		return original.replaceAll("\\S", ""+echoChar);
	}
}
