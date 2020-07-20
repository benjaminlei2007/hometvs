package com.oldwet.hometvs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.leanback.widget.Presenter;

public class ChannelPresenter extends Presenter {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_player_channel, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object o) {
        if (o instanceof Channel) {
            ViewHolder vh = (ViewHolder) viewHolder;
            vh.tvNumber.setText(String.valueOf(((Channel) o).getChannelNumber()));
            vh.tvChannel.setText(((Channel) o).getChannelName());
            vh.tvProgramTitle.setText("测试");
            vh.pbProgramProgress.setProgress(50);
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {

    }

    static class ViewHolder extends Presenter.ViewHolder {
        ImageView ivLogo;
        TextView tvNumber;
        TextView tvChannel;
        ImageView ivPlay;
        ImageView ivCatchup;
        TextView tvProgramTitle;
        ProgressBar pbProgramProgress;


        public ViewHolder(View view) {
            super(view);
            ivLogo = view.findViewById(R.id.ivLogo);
            tvNumber = view.findViewById(R.id.tvNumber);
            tvChannel = view.findViewById(R.id.tvChannel);
            ivPlay = view.findViewById(R.id.ivPlay);
            ivCatchup = view.findViewById(R.id.ivCatchup);
            tvProgramTitle = view.findViewById(R.id.tvProgramTitle);
            pbProgramProgress = view.findViewById(R.id.pbProgramProgress);
        }

    }
}
