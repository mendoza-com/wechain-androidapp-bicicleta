package com.boa.wechain.utils;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by Boa (David Figueroa dgfigueroa29@gmail.com) on 26 nov 2017.
 */
public interface RetrofitApiService{
	@Headers({
		"Accept: application/json",
		"Content-type: application/json",
		"Accept-Language: es",
		"Authorization: Bearer d32f7a8d983b442f608bcdbef27e41c32bf0d9a8"
	})
	@POST(Api.REGISTER)
	Call<ResponseBody> register(@Body UserParam param);
}