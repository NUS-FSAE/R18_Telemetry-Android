package nusfsae.r18telemetry;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by FSAE on 09-Mar-18.
 */

public class MyTab extends Fragment {
    private int layoutID = R.layout.driver_tab;
    private String title;

    public MyTab setLayout(int layoutID) {
        this.layoutID = layoutID;
        return this;
    }

    public MyTab setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getTitle() {
        return title;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(layoutID,container,false);
        return view;
    }
}
