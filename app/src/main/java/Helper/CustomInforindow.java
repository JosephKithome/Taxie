package Helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.taxie.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInforindow implements GoogleMap.InfoWindowAdapter
{
    View myview;

    public CustomInforindow(Context context) {
        myview = LayoutInflater.from(context)
                .inflate(R.layout.custom_rider_infor_window,null);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        TextView txtPickupTitle =((TextView)myview.findViewById(R.id.txtPickupInfo));
        txtPickupTitle.setText(marker.getTitle());
        TextView txtPickupSnippet =((TextView)myview.findViewById(R.id.txtPickupInfo));
        txtPickupSnippet.setText(marker.getSnippet());
        return myview;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
