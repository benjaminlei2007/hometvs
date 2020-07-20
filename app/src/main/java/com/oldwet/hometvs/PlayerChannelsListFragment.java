package com.oldwet.hometvs;

import android.os.Bundle;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.VerticalGridView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlayerChannelsListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayerChannelsListFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PlayerChannelsListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PlayerChannelsList.
     */
    // TODO: Rename and change types and number of parameters
    public static PlayerChannelsListFragment newInstance(String param1, String param2) {
        PlayerChannelsListFragment fragment = new PlayerChannelsListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_player_channels_list, container, false);
        // Inflate the layout for this fragment
        VerticalGridView vgvChannels=view.findViewById(R.id.vgvChannels);
        ArrayObjectAdapter arrayObjectAdapter = new ArrayObjectAdapter(new ChannelPresenter());
        ItemBridgeAdapter itemBridgeAdapter = new ItemBridgeAdapter(arrayObjectAdapter);
        /*FocusHighlightHelper.setupBrowseItemFocusHighlight(itemBridgeAdapter,
                FocusHighlight.ZOOM_FACTOR_LARGE, true);*/
        vgvChannels.setAdapter(itemBridgeAdapter);
        arrayObjectAdapter.addAll(0, ChannelModel.getChannelList());

        TextView textView=view.findViewById(R.id.tvPlaylistAndGroupName);
        textView.setText("所有频道");

        return view;


    }
}
