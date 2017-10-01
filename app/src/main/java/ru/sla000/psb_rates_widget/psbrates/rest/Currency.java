package ru.sla000.psb_rates_widget.psbrates.rest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Currency {

	@SerializedName("currencyId")
	@Expose
	public Integer currencyId;
	@SerializedName("name")
	@Expose
	public String name;
	@SerializedName("nameIso")
	@Expose
	public String nameIso;

}
