package ru.sla000.psb_rates_widget.psbrates.rest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CurrencyRate {

	@SerializedName("currency")
	@Expose
	public Currency currency;
	@SerializedName("multiplier")
	@Expose
	public Integer multiplier;
	@SerializedName("centralBankRate")
	@Expose
	public Float centralBankRate;
	@SerializedName("centralBankRateTrend")
	@Expose
	public Integer centralBankRateTrend;
	@SerializedName("sell")
	@Expose
	public Float sell;
	@SerializedName("buy")
	@Expose
	public Float buy;

}
