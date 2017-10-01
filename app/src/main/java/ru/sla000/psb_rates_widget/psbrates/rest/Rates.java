package ru.sla000.psb_rates_widget.psbrates.rest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by sla on 03.09.17.
 *
 * psb API
 */

public interface Rates {
	@GET("/api/currencyRates")
	Call<List<CurrencyRate>> getData();
}
