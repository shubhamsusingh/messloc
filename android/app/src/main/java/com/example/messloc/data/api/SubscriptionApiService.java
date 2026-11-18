
package com.example.messloc.data.api;

import com.example.messloc.data.model.admin.AdminApiResponse;
import com.example.messloc.data.model.subscription.CreateSubscriptionRequest;
import com.example.messloc.data.model.subscription.Subscription;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SubscriptionApiService {

    // Subscribe to a mess
    @POST("subscriptions")
    Call<AdminApiResponse<Subscription>> createSubscription(
            @Header("Authorization") String token,
            @Body CreateSubscriptionRequest request
    );

    // Get logged-in user's subscriptions
    @GET("subscriptions/me")
    Call<AdminApiResponse<List<Subscription>>> getMySubscriptions(
            @Header("Authorization") String token
    );

    // Cancel subscription
    @PATCH("subscriptions/{subscriptionId}/cancel")
    Call<AdminApiResponse<Subscription>> cancelSubscription(
            @Header("Authorization") String token,
            @Path("subscriptionId") String subscriptionId
    );
}
