package Common;

import Remote.IGoogleAPI;
import Remote.RetrofitClient;

public class Common {
    public  static  final  String driver_tbl ="Drivers";
    public  static  final  String user_driver_tbl ="Drivers";
    public  static  final  String user_rider_tbl ="Riders";
    public  static  final  String pickup_request_tbl ="PickupRequest";

    public  static  final String baseURL ="https://maps.googleapis.com";
    public  static IGoogleAPI getGoogleAPI()
    {
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }
}
