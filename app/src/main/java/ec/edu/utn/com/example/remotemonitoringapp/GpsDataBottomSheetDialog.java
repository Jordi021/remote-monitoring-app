package ec.edu.utn.com.example.remotemonitoringapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class GpsDataBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_LAST_LOCATION = "last_location";
    private static final String ARG_IS_COLLECTING = "is_collecting";

    private GpsDataListener listener;

    public interface GpsDataListener {
        void onSwitchChanged(boolean isChecked);
    }

    public void setGpsDataListener(GpsDataListener listener) {
        this.listener = listener;
    }

    public static GpsDataBottomSheetDialog newInstance(String lastLocation, boolean isCollecting) {
        GpsDataBottomSheetDialog fragment = new GpsDataBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_LAST_LOCATION, lastLocation);
        args.putBoolean(ARG_IS_COLLECTING, isCollecting);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_gps_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvLastLocation = view.findViewById(R.id.tv_last_location_modal);
        SwitchMaterial switchGps = view.findViewById(R.id.switch_gps_collection);

        if (getArguments() != null) {
            tvLastLocation.setText(getArguments().getString(ARG_LAST_LOCATION));
            switchGps.setChecked(getArguments().getBoolean(ARG_IS_COLLECTING));
        }

        switchGps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onSwitchChanged(isChecked);
            }
        });
    }
}