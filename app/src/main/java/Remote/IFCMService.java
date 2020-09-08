package Remote;

import Model.FCMResponse;
import Model.Sender;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key<AAAARu2gYqs:APA91bFLMUX6Sos8fKpHxbzuoLA4mZ3392uM1kVU8gvfUE5wFnQIgmsyljXjnBPwjTwRxJoZXOUjQeHsr4axKPzW9W5Sf-d1H9WoKRxLjWzd7-npF2LvhMiFpQ5DGV-6y9NCO0gW5nWg>"
    })
    @POST("fcm/send")
    Call<FCMResponse> sendMessage (@Body Sender body);
}
